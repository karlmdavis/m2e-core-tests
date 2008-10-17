/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Manages the installation/deinstallation of global actions for multi-page
 * editors. Responsible for the redirection of global actions to the active
 * editor. Multi-page contributor replaces the contributors for the individual
 * editors in the multi-page editor.
 */
public class MavenPomEditorContributor extends MultiPageEditorActionBarContributor {
  private ITextEditor sourceEditorPart;

  public void setActivePage(IEditorPart part) {
    //set the text editor
    if (part instanceof ITextEditor && sourceEditorPart == null) {
      sourceEditorPart = (ITextEditor) part;
    }

    IActionBars actionBars = getActionBars();
    if(sourceEditorPart !=null) {
      if (actionBars != null) {
        actionBars.clearGlobalActionHandlers();
        
        // undo/redo always enabled
        actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), //
            getAction(ITextEditorActionConstants.UNDO));
        actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), //
            getAction(ITextEditorActionConstants.REDO));
  
        // all other action, for text editor only (FormPage doesn't provide for these actions...)
        if (part instanceof ITextEditor) {
          actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), //
              getAction(ITextEditorActionConstants.DELETE));
          actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), //
              getAction(ITextEditorActionConstants.CUT));
          actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), //
              getAction(ITextEditorActionConstants.COPY));
          actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), //
              getAction(ITextEditorActionConstants.PASTE));
          actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), //
              getAction(ITextEditorActionConstants.SELECT_ALL));
          actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), //
              getAction(ITextEditorActionConstants.FIND));
          actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), //
              getAction(IDEActionFactory.BOOKMARK.getId()));
        }
        
        actionBars.updateActionBars();
      }
    }
    
    if(part instanceof DependencyGraphPage) {
      final DependencyGraphPage graphPage = (DependencyGraphPage) part;
      IAction printaAction = new Action() {
        public void run() {
          new DependencyGraphPrintDialog(graphPage.getEditorSite().getShell(), graphPage).open();
        }
      };

      if(actionBars!=null) {
        actionBars.setGlobalActionHandler(ActionFactory.PRINT.getId(), printaAction);
        actionBars.updateActionBars();
      }
    }
  }
  
  /**
   * Returns the action registered with the given text editor.
   * 
   * @return IAction or null if editor is null.
   */
  protected IAction getAction(String actionId) {
    return sourceEditorPart.getAction(actionId);
  }
  
}
