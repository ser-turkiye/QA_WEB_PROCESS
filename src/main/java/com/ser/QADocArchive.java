package com.ser;
import com.ser.evITAWeb.api.controls.ISelectionBox;
import com.ser.foldermanager.IFolder;
import utils.*;

import com.ser.blueline.*;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.ITaskDefinition;
import com.ser.evITAWeb.EvitaWebException;
import com.ser.evITAWeb.api.IDialog;
import com.ser.evITAWeb.api.actions.IBasicAction;
import com.ser.evITAWeb.api.actions.IMessageAction;
import com.ser.evITAWeb.api.actions.IStopFurtherAction;
import com.ser.evITAWeb.api.controls.IControl;
import com.ser.evITAWeb.api.controls.ITextField;
import com.ser.evITAWeb.scripting.Doxis4ClassFactory;
import com.ser.evITAWeb.scripting.bpmservice.task.TaskScripting;
import org.slf4j.Logger;

import java.util.*;

public class QADocArchive extends TaskScripting {


    private static Logger log;
    public ISession ses;
    private IDialog dlg;
    public QADocArchive(ITask task){
        super(task);
        this.log=super.log;
    }
    @Override
    public void onInit() throws EvitaWebException {

        ses = getTask().getSession();
        ITaskDefinition taskDefinition = getTask().getTaskDefinition();
        String activityName = taskDefinition.getName();

        processView.setDialogType("default");

    }
    public IStopFurtherAction stopFurtherAction(String etxt){
        IStopFurtherAction rtrn = Doxis4ClassFactory.createStopFurtherAction();
        rtrn.setMessage(etxt);
        rtrn.setType(IMessageAction.EnumMessageType.ERROR);
        return rtrn;
    }
    @Override
    public IBasicAction onCommit(IDialog dialog) throws EvitaWebException {
        if (dialog == null) {return null;}
        try {
            GeneralLib.session = getTask().getSession();
            XTRObjects.setSession(GeneralLib.session);

            String comp = "";
            IControl cntComp = dialog.getFieldByName(GeneralLib.Descriptors.CompCode);
            if (cntComp != null && cntComp instanceof ISelectionBox) {
                comp = Utils.getSelectionBoxValue(cntComp);
            }
            if (comp.isEmpty()) {
                throw new Exception("Invalid Company. Kindly re-check your inputs");
            }

            IInformationObject qaInfObj = GeneralLib.getQAWorkspace(comp);
            if(qaInfObj == null){
                throw new Exception("Company not found. [" + comp + "]");
            }

            List<ILink> attachLink = getTask().getProcessInstance().getLoadedInformationObjectLinks().getLinks();
            if(attachLink.isEmpty()){
                throw new Exception("Attachment list is empty.");
            }
            if(attachLink.size() > 1){
                throw new Exception("Please select only one document.");
            }

            IInformationObject parent = attachLink.get(0).getTargetInformationObject();
            String psts = parent.getDescriptorValue(GeneralLib.Descriptors.Status, String.class);
            psts = (psts == null ?  "" : psts);

            if(!psts.equals("PUBLISH")){
                throw new Exception("Please select published document.");
            }

            IControl cntDocId = dialog.getFieldByName(GeneralLib.Descriptors.DocId);
            if (cntDocId != null && cntDocId instanceof ITextField) {
                ITextField fldReqId = (ITextField) cntDocId;
                String docId = GeneralLib.getQAReqId(qaInfObj, comp, fldReqId.getText());
                Utils.setText(dlg, cntDocId.getName(), docId);
            }

            GeneralLib.disconnectToFolder((IFolder) qaInfObj, "Dokümanlar", "İçe Aktarılanlar", parent);

        }catch(Exception ex){
            log.error("Exception       : " + ex.getMessage());
            log.error("    Class       : " + ex.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(ex.getStackTrace()));
            return stopFurtherAction(ex.getMessage());
        }

        return null;
    }

    @Override
    public void onInitMetadataDialog(IDialog dialog) throws EvitaWebException {
        GeneralLib.session = getTask().getSession();
        this.dlg = dialog;

        IUser usr = getTask().getCreator();
        if(usr==null) usr = GeneralLib.session.getUser();

        List<IInformationObject> updateList = new ArrayList<>();
        IInformationObject parent = getTask().getProcessInstance().getMainInformationObject();

        if(parent == null){
            List<ILink> attachLink = getTask().getProcessInstance().getLoadedInformationObjectLinks().getLinks();
            parent = attachLink.isEmpty() ? parent : attachLink.get(0).getTargetInformationObject();
        }

        String comp = "";
        comp = parent.getDescriptorValue("OrgCompanyDescription");

        updateList.add(parent);
        IInformationObject qaInfObj = (comp.isEmpty() ? null :GeneralLib.getQAWorkspace(comp));
        if(qaInfObj !=null ){updateList.add(qaInfObj);}

        for(IInformationObject sourceObje : updateList){
            Vector<IControl> fields = dlg.getFields();
            for(IControl ctrl : fields){
                if(!ctrl.isReadonly()) continue;

                if (ctrl.getName() == null || ctrl.getName().isEmpty()) continue;
                if (ctrl.getDescriptorId() == null || ctrl.getDescriptorId().isEmpty()) continue;

                if (ctrl.getName().contains("ObjectDocID") || ctrl.getName().contains("ObjectDocID")){
                    continue;
                }
                String descID = ctrl.getDescriptorId();
                String parentVal = sourceObje.getDescriptorValue(descID);
                if(parentVal == null) continue;
                if(parentVal.isEmpty()) continue;

                Utils.setText(dlg , ctrl.getName() , parentVal);
            }
        }

        super.onInitMetadataDialog(dialog);
    }

}
