
package org.eclipse.m2e.integration.tests;

import static org.eclipse.m2e.integration.tests.common.ContextMenuHelper.clickContextMenu;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.integration.tests.common.SwtbotUtil;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Test;


public class MngEclipseRefactorRenameMultiProjectTest extends M2EUIIntegrationTestCase {

  @Test
  public void testMngEclipseRefactorRenameMultiProject() throws Exception {

    importMavenProjects("projects/refactor_multiprojects.zip");
    IMavenProjectFacade mavenProject = MavenPlugin.getMavenProjectRegistry().getMavenProject(
        "org.sonatype.test", "someproject", "0.0.1-SNAPSHOT");
    assertNotNull(mavenProject);

    waitForAllBuildsToComplete();

    clickContextMenu(selectProject("Dep"), new String[] {"Refactor", "Rename Maven Artifact..."});

    SWTBotShell shell = bot.shell("Rename Maven Artifact");
    try {
      shell.activate();

      bot.textWithLabel(Messages.artifactComponentGroupId).setText("x.y.z");
      bot.textWithLabel(Messages.artifactComponentArtifactId).setText("project2");
      bot.textWithLabel(Messages.artifactComponentVersion).setText("1.1.1");
      bot.checkBox("Rename Eclipse project in Workspace").select();
      bot.button("OK").click();
    } finally {
      SwtbotUtil.waitForClose(shell);
    }

    waitForAllBuildsToComplete();

    IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject("project2");
    assertTrue("project2 is expected to exist in the workspace", project2.exists());
    mavenProject = MavenPlugin.getMavenProjectRegistry().getMavenProject("org.sonatype.test", "Dep",
        "0.0.1-SNAPSHOT");
    assertNull("Paven project \"Dep\" should not exist anymore", mavenProject);
    IMavenProjectFacade mavenProject2 = MavenPlugin.getMavenProjectRegistry().getMavenProject("x.y.z",
        "project2", "1.1.1");
    assertNotNull("Maven project \"project2\" is expected to exist", mavenProject2);
  }

}
