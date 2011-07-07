/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.integration.tests;

import static org.hamcrest.Matchers.startsWith;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndex;
import org.eclipse.m2e.core.internal.index.nexus.NexusIndexManager;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.m2e.integration.tests.common.matchers.ContainsMnemonic;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author Rich Seddon
 */
@Ignore
@SuppressWarnings("restriction")
public class MEclipse163ResolveDependenciesTest extends M2EUIIntegrationTestCase {

  private String projectName = "ResolveDepProject";

  private String oldUserSettings;

  @Before
    public void setUp()
        throws Exception
    {
    oldUserSettings = setUserSettings("resources/settings.xml");
  }

  @After
    public void tearDown()
        throws Exception
    {
    setUserSettings(oldUserSettings);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testResolveDependencies() throws Exception {
    importZippedProject("projects/resolve_deps_test.zip");
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    Assert.assertTrue(project.exists());
    waitForAllBuildsToComplete();
    //rebuild the mirror
    IRepositoryRegistry registry = MavenPlugin.getRepositoryRegistry();
    waitForAllBuildsToComplete();
    List<IRepository> repos = registry.getRepositories(IRepositoryRegistry.SCOPE_SETTINGS);

    for(IRepository repo : repos) {
      if(repo.getUrl().endsWith("resources/remote-repo")) {
        buildFullRepoDetails(repo);
      }
    }

    // there should be compile errors
    int severity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    Assert.assertEquals(IMarker.SEVERITY_ERROR, severity);

    SWTBotEclipseEditor editor = bot.editorByTitle(
        openFile(project, "src/main/java/org/sonatype/test/ResolveDepProject/App.java").getTitle()).toTextEditor();

    editor.pressShortcut(SWT.MOD1, '.'); // next annotation

    editor.pressShortcut(SWT.MOD1, '1');
    editor.pressShortcut(KeyStroke.getInstance(SWT.END));
    editor.pressShortcut(KeyStroke.getInstance(SWT.ARROW_UP));
    editor.pressShortcut(KeyStroke.getInstance(SWT.LF));

    SWTBotShell shell = bot.shell("Search in Maven repositories");
    try {
      shell.activate();

      String packaging = "junit.framework";
      String groupId = "junit";
      String artifactId = "junit";
      String version = "3.8.2";
      bot.text().setText("AssertionFailedError");
      SWTBotTreeItem node = bot.tree().getTreeItem(ContainsMnemonic.containsMnemonic(packaging),
          ContainsMnemonic.containsMnemonic(groupId), ContainsMnemonic.containsMnemonic(artifactId));
      node.expand();
      node.select(findNodeName(node, startsWith(version)));

      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }
    bot.sleep(100);

    waitForAllBuildsToComplete();

    assertProjectsHaveNoErrors();

  }

  /**
   * For any class details, we need a FULL_DETAILS
   * 
   * @param repo
   */
  private void buildFullRepoDetails(IRepository repo) throws Exception {
    NexusIndexManager indexManager = (NexusIndexManager) MavenPlugin.getIndexManager();
    NexusIndex index = indexManager.getIndex(repo);
    IRepositoryRegistry registry = MavenPlugin.getRepositoryRegistry();

    //build full repo details for the enabled non local/workspace repo
    if(index.isEnabled() && !(repo.equals(registry.getLocalRepository()))
        && !(repo.equals(registry.getWorkspaceRepository()))) {
      indexManager.setIndexDetails(repo, "full", monitor);
      indexManager.updateIndex(repo, true, monitor);
      waitForAllBuildsToComplete();
    }
  }
}
