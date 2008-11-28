/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import org.eclipse.core.runtime.CoreException;

import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * IClasspathCollector
 *
 * @author igor
 */
public interface IClasspathCollector {

  public void addProjectEntry(IMavenProjectFacade facade);
  
  public void addArchiveEntry(String entry) throws CoreException;
}
