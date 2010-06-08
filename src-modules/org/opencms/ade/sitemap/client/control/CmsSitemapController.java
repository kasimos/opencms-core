/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/sitemap/client/control/Attic/CmsSitemapController.java,v $
 * Date   : $Date: 2010/06/08 14:35:17 $
 * Version: $Revision: 1.4 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.sitemap.client.control;

import org.opencms.ade.sitemap.client.Messages;
import org.opencms.ade.sitemap.client.model.CmsClientSitemapChangeDelete;
import org.opencms.ade.sitemap.client.model.CmsClientSitemapChangeEdit;
import org.opencms.ade.sitemap.client.model.CmsClientSitemapChangeMove;
import org.opencms.ade.sitemap.client.model.CmsClientSitemapChangeMoveDnD;
import org.opencms.ade.sitemap.client.model.CmsClientSitemapChangeNew;
import org.opencms.ade.sitemap.client.model.I_CmsClientSitemapChange;
import org.opencms.ade.sitemap.shared.CmsClientSitemapEntry;
import org.opencms.ade.sitemap.shared.CmsSitemapData;
import org.opencms.ade.sitemap.shared.CmsSitemapTemplate;
import org.opencms.ade.sitemap.shared.CmsSubSitemapInfo;
import org.opencms.ade.sitemap.shared.rpc.I_CmsSitemapService;
import org.opencms.ade.sitemap.shared.rpc.I_CmsSitemapServiceAsync;
import org.opencms.file.CmsResource;
import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.rpc.CmsRpcAction;
import org.opencms.gwt.client.rpc.CmsRpcPrefetcher;
import org.opencms.gwt.client.ui.CmsNotification;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.sitemap.CmsSitemapManager;
import org.opencms.xml.sitemap.I_CmsSitemapChange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;

/**
 * Sitemap editor controller.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.4 $ 
 * 
 * @since 8.0.0
 */
public class CmsSitemapController {

    /** The list of changes. */
    protected List<I_CmsClientSitemapChange> m_changes;

    /** The sitemap data. */
    protected CmsSitemapData m_data;

    /** The handler manager. */
    protected HandlerManager m_handlerManager;

    /** The list of undone changes. */
    protected List<I_CmsClientSitemapChange> m_undone;

    /** The set of names of hidden properties. */
    private Set<String> m_hiddenProperties;

    /** The sitemap service instance. */
    private I_CmsSitemapServiceAsync m_service;

    /**
     * Constructor.<p>
     */
    public CmsSitemapController() {

        m_changes = new ArrayList<I_CmsClientSitemapChange>();
        m_undone = new ArrayList<I_CmsClientSitemapChange>();
        m_data = (CmsSitemapData)CmsRpcPrefetcher.getSerializedObject(getService(), CmsSitemapData.DICT_NAME);

        m_hiddenProperties = new HashSet<String>();
        m_hiddenProperties.add(CmsSitemapManager.Property.template.toString());
        m_hiddenProperties.add(CmsSitemapManager.Property.templateInherited.toString());
        m_hiddenProperties.add(CmsSitemapManager.Property.sitemap.toString());
        m_handlerManager = new HandlerManager(this);
    }

    /**
     * Returns the URI of the current sitemap.<p>
     * 
     * @return the URI of the current sitemap 
     */
    protected static String getSitemapUri() {

        return CmsCoreProvider.get().getUri();

    }

    /**
     * Adds a new change event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addChangeHandler(I_CmsSitemapChangeHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapChangeEvent.getType(), handler);
    }

    /**
     * Adds a new clear undo event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addClearUndoHandler(I_CmsSitemapClearUndoHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapClearUndoEvent.getType(), handler);
    }

    /**
     * Adds a new first undo event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addFirstUndoHandler(I_CmsSitemapFirstUndoHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapFirstUndoEvent.getType(), handler);
    }

    /**
     * Adds a new last redo event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addLastRedoHandler(I_CmsSitemapLastRedoHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapLastRedoEvent.getType(), handler);
    }

    /**
     * Adds a new last undo event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addLastUndoHandler(I_CmsSitemapLastUndoHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapLastUndoEvent.getType(), handler);
    }

    /**
     * Adds a new load event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addLoadHandler(I_CmsSitemapLoadHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapLoadEvent.getType(), handler);
    }

    /**
     * Adds a new reset event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addResetHandler(I_CmsSitemapResetHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapResetEvent.getType(), handler);
    }

    /**
     * Adds a new start edit event handler.<p>
     * 
     * @param handler the handler to add
     * 
     * @return the handler registration
     */
    public HandlerRegistration addStartEdiHandler(I_CmsSitemapStartEditHandler handler) {

        return m_handlerManager.addHandler(CmsSitemapStartEditEvent.getType(), handler);
    }

    /**
     * Commits the changes.<p>
     * 
     * @param sync if to use a synchronized or an asynchronized request  
     */
    public void commit(final boolean sync) {

        commit(sync, null, true);
    }

    /**
     * Registers a new sitemap entry.<p>
     * 
     * @param newEntry the new entry
     */
    public void create(CmsClientSitemapEntry newEntry) {

        assert (getEntry(newEntry.getSitePath()) == null);
        assert (getEntry(CmsResource.getParentFolder(newEntry.getSitePath())) != null);

        newEntry.setPosition(-1); // ensure it will be inserted at the end
        addChange(new CmsClientSitemapChangeNew(newEntry), false);
    }

    /**
     * Creates a sub-sitemap from the subtree of the current sitemap starting at a given path.<p>
     * 
     * @param path the path whose subtree should be converted to a sub-sitemap 
     */
    public void createSubSitemap(final String path) {

        CmsRpcAction<CmsSubSitemapInfo> subSitemapAction = new CmsRpcAction<CmsSubSitemapInfo>() {

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
             */
            @Override
            public void execute() {

                start(0);
                getService().createSubsitemap(getSitemapUri(), path, this);
            }

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
             */
            @Override
            protected void onResponse(CmsSubSitemapInfo result) {

                stop(false);
                onCreateSubSitemap(path, result);

            }
        };
        if (CmsCoreProvider.get().lockAndCheckModification(getSitemapUri(), m_data.getTimestamp())) {
            commit(false, subSitemapAction, false);
        }
    }

    /**
     * Deletes the given entry and all its descendants.<p>
     * 
     * @param sitePath the site path of the entry to delete
     */
    public void delete(String sitePath) {

        CmsClientSitemapEntry entry = getEntry(sitePath);
        assert (entry != null);
        addChange(new CmsClientSitemapChangeDelete(entry), false);
    }

    /**
     * Edits the given sitemap entry.<p>
     * 
     * @param entry the sitemap entry to update
     * @param title the new title, can be <code>null</code> to keep the old one
     * @param vfsReference the new VFS reference, can be <code>null</code> to keep the old one
     * @param properties the new properties, can be <code>null</code> to keep the old properties
     */
    public void edit(CmsClientSitemapEntry entry, String title, String vfsReference, Map<String, String> properties) {

        // check changes
        boolean changedTitle = ((title != null) && !title.trim().equals(entry.getTitle()));
        boolean changedVfsRef = ((vfsReference != null) && !vfsReference.trim().equals(entry.getVfsPath()));
        boolean changedProperties = false;
        if (properties != null) {
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                String newValue = prop.getValue();
                String value = entry.getProperties().get(prop.getKey());
                if (newValue == null) {
                    if (value != null) {
                        changedProperties = true;
                        break;
                    }
                } else {
                    if (value == null) {
                        // check default value
                        value = m_data.getProperties().get(prop.getKey()).getDefault();
                    }
                    if (!newValue.equals(value)) {
                        changedProperties = true;
                        break;
                    }
                }
            }
        }
        if (!changedTitle && !changedVfsRef && !changedProperties) {
            // nothing to do
            return;
        }

        // create changes
        CmsClientSitemapEntry newEntry = new CmsClientSitemapEntry(entry);
        if (changedTitle) {
            newEntry.setTitle(title);
        }
        if (changedVfsRef) {
            newEntry.setVfsPath(vfsReference);
        }
        if (changedProperties) {
            // to preserve the hidden properties (navigation, sitemap...), we only copy the new property values
            newEntry.getProperties().putAll(properties);
        }

        // apply changes
        addChange(new CmsClientSitemapChangeEdit(entry, newEntry), false);
    }

    /**
     * Retrieves the children entries of the given node from the server.<p>
     * 
     * @param originalPath the original site path of the sitemap entry to get the children for
     * @param sitePath the current site path, in case if has been moved or renamed
     */
    public void getChildren(final String originalPath, final String sitePath) {

        CmsRpcAction<List<CmsClientSitemapEntry>> getChildrenAction = new CmsRpcAction<List<CmsClientSitemapEntry>>() {

            /**
            * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
            */
            @Override
            public void execute() {

                // Make the call to the sitemap service
                start(500);

                getService().getChildren(getSitemapUri(), originalPath, this);
            }

            /**
            * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
            */
            @Override
            public void onResponse(List<CmsClientSitemapEntry> result) {

                CmsClientSitemapEntry target = getEntry(sitePath);
                if (target == null) {
                    // this might happen after an automated deletion 
                    stop(false);
                    return;
                }
                target.setSubEntries(result);
                if (!originalPath.equals(sitePath)) {
                    target.setSitePath("abc"); // hack to be able to execute updateSitePath
                    target.updateSitePath(sitePath);
                }
                m_handlerManager.fireEvent(new CmsSitemapLoadEvent(target, originalPath));
                stop(false);
            }
        };
        getChildrenAction.execute();
    }

    /**
     * Returns the sitemap data.<p>
     *
     * @return the sitemap data
     */
    public CmsSitemapData getData() {

        return m_data;
    }

    /**
     * This method returns the default template for a given sitemap path.<p>
     * 
     * Starting from the given path, it traverses the ancestors of the entry to find a sitemap 
     * entry with a non-null 'template-inherited' property value, and then returns this value.
     * 
     * @param sitemapPath the sitemap path for which the default template should be returned
     *  
     * @return the default template 
     */
    public CmsSitemapTemplate getDefaultTemplate(String sitemapPath) {

        if ((sitemapPath == null) || sitemapPath.equals("") || sitemapPath.equals("/")) {
            return m_data.getDefaultTemplate();
        }

        CmsClientSitemapEntry entry = getEntry(sitemapPath);
        String templateInherited = entry.getProperties().get(CmsSitemapManager.Property.templateInherited);
        if (templateInherited != null) {
            return m_data.getTemplates().get(templateInherited);
        }

        if (sitemapPath.equals(m_data.getRoot().getSitePath())) {
            return m_data.getDefaultTemplate();
        }
        String parentPath = CmsResource.getParentFolder(sitemapPath);
        return getDefaultTemplate(parentPath);
    }

    /**
     * Returns the tree entry with the given path.<p>
     * 
     * @param entryPath the path to look for
     * 
     * @return the tree entry with the given path, or <code>null</code> if not found
     */
    public CmsClientSitemapEntry getEntry(String entryPath) {

        CmsClientSitemapEntry root = m_data.getRoot();
        if (!entryPath.startsWith(root.getSitePath())) {
            return null;
        }
        String path = entryPath.substring(root.getSitePath().length());
        String[] names = CmsStringUtil.splitAsArray(path, "/");
        CmsClientSitemapEntry result = root;
        for (String name : names) {
            if (CmsStringUtil.isEmptyOrWhitespaceOnly(name)) {
                // in case of leading slash
                continue;
            }
            boolean found = false;
            for (CmsClientSitemapEntry child : result.getSubEntries()) {
                if (child.getName().equals(name)) {
                    found = true;
                    result = child;
                    break;
                }
            }
            if (!found) {
                // not found
                result = null;
                break;
            }
        }
        return result;
    }

    /**
     * Checks if any change made.<p>
     * 
     * @return <code>true</code> if there is at least a change to commit
     */
    public boolean isDirty() {

        return !m_changes.isEmpty();
    }

    /**
     * Checks if the current sitemap is editable.<p>
     *
     * @return <code>true</code> if the current sitemap is editable
     */
    public boolean isEditable() {

        return CmsStringUtil.isEmptyOrWhitespaceOnly(m_data.getNoEditReason());
    }

    /**
     * Checks whether a string is the name of a hidden property.<p>
     * 
     * A hidden property is a property which should not appear in the property editor
     * because it requires special treatment.<p>
     * 
     * @param propertyName the property name which should be checked
     * 
     * @return true if the argument is the name of a hidden property
     */
    public boolean isHiddenProperty(String propertyName) {

        return m_hiddenProperties.contains(propertyName);
    }

    /**
     * Checks if the given site path is the sitemap root.<p>
     * 
     * @param sitePath the site path to check
     * 
     * @return <code>true</code> if the given site path is the sitemap root
     */
    public boolean isRoot(String sitePath) {

        return m_data.getRoot().getSitePath().equals(sitePath);
    }

    /**
     * Moves the given sitemap entry with all its descendants to the new position.<p>
     * 
     * @param entry the sitemap entry to move
     * @param toPath the destination path
     * @param position the new position between its siblings
     */
    public void move(CmsClientSitemapEntry entry, String toPath, int position) {

        assert (getEntry(entry.getSitePath()) != null);
        if ((toPath == null) || (entry.getSitePath().equals(toPath) && (entry.getPosition() == position))) {
            // nothing to do
            return;
        }
        assert (getEntry(CmsResource.getParentFolder(toPath)) != null);

        addChange(new CmsClientSitemapChangeMove(entry.getSitePath(), entry.getPosition(), toPath, position), false);
    }

    /**
     * Moves the given sitemap entry with all its descendants to the new position with drag and drop.<p>
     * 
     * @param entry the sitemap entry to move
     * @param toPath the destination path
     * @param position the new position between its siblings
     */
    public void moveDnd(CmsClientSitemapEntry entry, String toPath, int position) {

        assert (getEntry(entry.getSitePath()) != null);
        if ((toPath == null) || (entry.getSitePath().equals(toPath) && (entry.getPosition() == position))) {
            // nothing to do
            return;
        }
        assert (getEntry(CmsResource.getParentFolder(toPath)) != null);

        addChange(new CmsClientSitemapChangeMoveDnD(entry.getSitePath(), entry.getPosition(), toPath, position), false);
    }

    /**
     * Re-does the last undone change.<p>
     */
    public void redo() {

        if (m_undone.isEmpty()) {
            return;
        }

        // redo
        I_CmsClientSitemapChange change = m_undone.remove(m_undone.size() - 1);
        addChange(change, true);

        // state
        if (m_undone.isEmpty()) {
            m_handlerManager.fireEvent(new CmsSitemapLastRedoEvent());
        }
    }

    /**
     * Discards all changes, even unlocking the sitemap resource.<p>
     */
    public void reset() {

        m_changes.clear();
        m_undone.clear();

        // state
        m_handlerManager.fireEvent(new CmsSitemapResetEvent());

        CmsCoreProvider.get().unlock();
        Window.Location.reload();
    }

    /**
     * Undoes the last change.<p>
     */
    public void undo() {

        if (!isDirty()) {
            return;
        }

        // pre-state
        if (m_undone.isEmpty()) {
            m_handlerManager.fireEvent(new CmsSitemapFirstUndoEvent());
        }

        // undo
        I_CmsClientSitemapChange change = m_changes.remove(m_changes.size() - 1);
        m_undone.add(change.getChangeForUndo());

        // update data
        I_CmsClientSitemapChange revertChange = change.revert();
        revertChange.applyToModel(this);

        // refresh view
        m_handlerManager.fireEvent(new CmsSitemapChangeEvent(revertChange));

        // post-state
        if (!isDirty()) {
            m_handlerManager.fireEvent(new CmsSitemapLastUndoEvent());
            CmsCoreProvider.get().unlock();
        }
    }

    /**
     * Returns the sitemap service instance.<p>
     * 
     * @return the sitemap service instance
     */
    protected I_CmsSitemapServiceAsync getService() {

        if (m_service == null) {
            m_service = GWT.create(I_CmsSitemapService.class);
        }
        return m_service;
    }

    /**
     * Internal method which is called when a new sub-sitemap has been successfully created.<p>
     * 
     * @param path the path in the current sitemap at which the sub-sitemap has been created
     * @param info the info bean which is the result of the sub-sitemap creation  
    
     */
    protected void onCreateSubSitemap(String path, CmsSubSitemapInfo info) {

        String subSitemapPath = info.getSitemapPath();
        m_data.setTimestamp(info.getParentTimestamp());

        CmsClientSitemapEntry entry = getEntry(path);
        List<I_CmsClientSitemapChange> changes = new ArrayList<I_CmsClientSitemapChange>();
        for (CmsClientSitemapEntry childEntry : entry.getSubEntries()) {
            CmsClientSitemapChangeDelete deleteAction = new CmsClientSitemapChangeDelete(childEntry);
            changes.add(deleteAction);
        }
        CmsClientSitemapEntry newEntry = new CmsClientSitemapEntry(entry);
        newEntry.getProperties().put(CmsSitemapManager.Property.sitemap.name(), subSitemapPath);
        CmsClientSitemapChangeEdit editAction = new CmsClientSitemapChangeEdit(entry, newEntry);
        changes.add(editAction);
        executeChanges(changes);
    }

    /**
    * Adds a change to the queue.<p>
    * 
    * @param change the change to be added  
    * @param redo if redoing a change
    */
    private void addChange(I_CmsClientSitemapChange change, boolean redo) {

        // state
        if (!isDirty()) {
            if (CmsCoreProvider.get().lockAndCheckModification(getSitemapUri(), m_data.getTimestamp())) {
                m_handlerManager.fireEvent(new CmsSitemapStartEditEvent());
            } else {
                // could not lock
                return;
            }
        }

        if (!redo && !m_undone.isEmpty()) {
            // after a new change no changes can be redone
            m_undone.clear();
            m_handlerManager.fireEvent(new CmsSitemapClearUndoEvent());
        }

        // add it
        m_changes.add(change);

        // apply change to the model
        change.applyToModel(this);

        // refresh view, in dnd mode view already ok
        m_handlerManager.fireEvent(new CmsSitemapChangeEvent(change));
    }

    /**
     * Commits the changes.<p>
     * 
     * @param sync if to use a synchronized or an asynchronized request
     * @param nextAction if not null, this action will be executed after the sitemap has been saved (successfully)
     * @param unlockAfterSave if true, unlocks the sitemap after saving  (if sync is true, however, the sitemap will always be unlocked)
     */
    private void commit(final boolean sync, final CmsRpcAction<?> nextAction, final boolean unlockAfterSave) {

        // save the sitemap
        CmsRpcAction<Long> saveAction = new CmsRpcAction<Long>() {

            /**
            * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
            */
            @Override
            public void execute() {

                start(0);
                List<I_CmsSitemapChange> changes = new ArrayList<I_CmsSitemapChange>();
                for (I_CmsClientSitemapChange change : m_changes) {
                    changes.add(change.getChangeForCommit());
                }
                if (sync) {
                    getService().saveSync(getSitemapUri(), changes, this);
                } else {
                    getService().save(getSitemapUri(), changes, unlockAfterSave, this);
                }
            }

            /**
            * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
            */
            @Override
            public void onResponse(Long result) {

                m_data.setTimestamp(result.longValue());

                m_changes.clear();
                m_undone.clear();

                // state
                m_handlerManager.fireEvent(new CmsSitemapResetEvent());

                stop(true);
                if (nextAction != null) {
                    nextAction.execute();
                }
            }

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#show()
             */
            @Override
            protected void show() {

                CmsNotification.get().sendSticky(CmsNotification.Type.NORMAL, Messages.get().key(Messages.GUI_SAVING_0));
            }
        };
        saveAction.execute();
    }

    /**
     * Internal method which updates the model with a single change.<p>
     * 
     * @param change the change 
     */
    private void executeChange(I_CmsClientSitemapChange change) {

        // apply change to the model
        change.applyToModel(this);
        // refresh view
        m_handlerManager.fireEvent(new CmsSitemapChangeEvent(change));
    }

    /**
     * Internal method which updates the model with a list of changes.<p>
     * 
     * @param changes the list of changes 
     */
    private void executeChanges(List<I_CmsClientSitemapChange> changes) {

        for (I_CmsClientSitemapChange change : changes) {
            executeChange(change);

        }
    }
}