/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * CustomizableLifecycleMapping
 * 
 * @author igor
 */
public class CustomizableLifecycleMapping extends AbstractLifecycleMapping implements ILifecycleMapping {
  public static final String EXTENSION_ID = "customizable"; //$NON-NLS-1$

  public CustomizableLifecycleMapping() {
  }
  
  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    Plugin plugin = mavenProject.getPlugin("org.eclipse.m2e:lifecycle-mapping"); //$NON-NLS-1$

    if(plugin == null) {
      throw new IllegalArgumentException("no mapping");
    }

    // TODO assert version

    Map<String, AbstractProjectConfigurator> configuratorsMap = new LinkedHashMap<String, AbstractProjectConfigurator>();
    for(AbstractProjectConfigurator configurator : getProjectConfigurators()) {
      configuratorsMap.put(configurator.getId(), configurator);
    }

    Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();

    if(config == null) {
      throw new IllegalArgumentException("Empty lifecycle mapping configuration");
    }

    Xpp3Dom configuratorsDom = config.getChild("configurators"); //$NON-NLS-1$
    Xpp3Dom executionsDom = config.getChild("mojoExecutions"); //$NON-NLS-1$

    List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();
    
    if (configuratorsDom != null) {
      for(Xpp3Dom configuratorDom : configuratorsDom.getChildren("configurator")) { //$NON-NLS-1$
        String configuratorId = configuratorDom.getAttribute("id"); //$NON-NLS-1$
        AbstractProjectConfigurator configurator = configuratorsMap.get(configuratorId);
        if(configurator == null) {
          String message = "Configurator '"+configuratorId+"' is not available for project '"+facade.getProject().getName()+"'. To enable full functionality, install the configurator and run Maven->Update Project Configuration.";
          MavenPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, IMavenConstants.PLUGIN_ID, message));
          MavenPlugin.getDefault().getConsole().logError(message);
//          throw new IllegalArgumentException(message);
        }else{
          configurators.add(configurator);
        }
      }
    }
    
    if (executionsDom != null) {
      for(Xpp3Dom execution : executionsDom.getChildren("mojoExecution")) { //$NON-NLS-1$
        String strRunOnIncremental = execution.getAttribute("runOnIncremental"); //$NON-NLS-1$
        configurators.add(MojoExecutionProjectConfigurator.fromString(execution.getValue(), toBool(strRunOnIncremental, true)));
      }
    }

    return configurators;
  }
  
  private boolean toBool(String value, boolean def) {
    if(value == null || value.length() == 0) {
      return def;
    }
    return Boolean.parseBoolean(value);
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    List<AbstractProjectConfigurator> configurators = getProjectConfigurators(facade, monitor);

    return getBuildParticipants(facade, configurators, monitor);
  }
  
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    super.configure(request, monitor);

    addMavenBuilder(request.getProject(), monitor);
  }
  
  public List<String> getPotentialMojoExecutionsForBuildKind(IMavenProjectFacade projectFacade, int kind,
      IProgressMonitor progressMonitor) {
    List<String> mojos = new LinkedList<String>();
    try {
      for (MojoExecution execution : projectFacade.getExecutionPlan(progressMonitor).getExecutions()) {
        for (AbstractProjectConfigurator configurator : getProjectConfigurators(projectFacade, progressMonitor)) {
          AbstractBuildParticipant participant = configurator.getBuildParticipant(execution);
          if (participant != null && participant instanceof MojoExecutionBuildParticipant) {
            if(((MojoExecutionBuildParticipant)participant).appliesToBuildKind(kind)) {
              MojoExecution mojo = ((MojoExecutionBuildParticipant)participant).getMojoExecution();
              mojos.add(MojoExecutionUtils.getExecutionKey(mojo));
            }
          }
        }
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return mojos;
  }

  private static final String[] INTERESTING_PHASES = {"validate", //
      "initialize", //
      "generate-sources", //
      "process-sources", //
      "generate-resources", //
      "process-resources", //
      "compile", //
      "process-classes", //
      "generate-test-sources", //
      "process-test-sources", //
      "generate-test-resources", //
      "process-test-resources", //
      "test-compile", //
      "process-test-classes", //
  // "test", //
  // "prepare-package", //
  // "package", //
  //"pre-integration-test", //
  // "integration-test", //
  // "post-integration-test", //
  // "verify", //
  // "install", //
  // "deploy", //
  };

  public boolean isInterestingPhase(String phase) {
    for(String interestingPhase : INTERESTING_PHASES) {
      if(interestingPhase.equals(phase)) {
        return true;
      }
    }
    return false;
  }
}
