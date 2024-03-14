package com.ser;

import com.ser.blueline.IInformationObject;
import com.ser.blueline.ILink;
import com.ser.blueline.ISession;
import com.ser.blueline.IUser;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.ITaskDefinition;
import com.ser.evITAWeb.EvitaWebException;
import com.ser.evITAWeb.api.IDialog;
import com.ser.evITAWeb.api.actions.IBasicAction;
import com.ser.evITAWeb.api.actions.IMessageAction;
import com.ser.evITAWeb.api.actions.IStopFurtherAction;
import com.ser.evITAWeb.api.controls.IControl;
import com.ser.evITAWeb.api.controls.ISelectionBox;
import com.ser.evITAWeb.api.controls.ITextField;
import com.ser.evITAWeb.scripting.Doxis4ClassFactory;
import com.ser.evITAWeb.scripting.bpmservice.task.TaskScripting;
import com.ser.foldermanager.IFolder;
import org.slf4j.Logger;
import utils.GeneralLib;
import utils.Utils;
import utils.XTRObjects;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class QACapaDraft extends TaskScripting {


    private static Logger log;
    public ISession ses;
    private IDialog dlg;
    public QACapaDraft(ITask task){
        super(task);
        this.log=super.log;
    }
    @Override
    public void onInit() throws EvitaWebException {

        ses = getTask().getSession();
        ITaskDefinition taskDefinition = getTask().getTaskDefinition();
        String activityName = taskDefinition.getName();

        //processView.setDialogType("new");

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
//            if(attachLink.isEmpty()){
//                throw new Exception("Attachment list is empty.");
//            }

//            IInformationObject parent = attachLink.get(0).getTargetInformationObject();
//            String psts = parent.getDescriptorValue(GeneralLib.Descriptors.Status, String.class);
//            psts = (psts == null ?  "" : psts);
//
//            if(!psts.equals("PUBLISH")){
//                throw new Exception("Please select published document.");
//            }

//            IControl cntDocId = dialog.getFieldByName(GeneralLib.Descriptors.DocId);
//            if (cntDocId != null && cntDocId instanceof ITextField) {
//                ITextField fldReqId = (ITextField) cntDocId;
//                String docId = GeneralLib.getQAReqId(qaInfObj, comp, fldReqId.getText());
//                Utils.setText(dlg, cntDocId.getName(), docId);
//            }
//
//            GeneralLib.disconnectToFolder((IFolder) qaInfObj, "Dokümanlar", "İçe Aktarılanlar", parent);

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

        Date today = new Date();
        Date tomorrow = new Date(today.getTime() + (1000 * 60 * 60 * 24));
        String cDate = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        String fDate = new SimpleDateFormat("dd.MM.yyyy").format(tomorrow);

        Utils.setText(dlg , "DateStart" , cDate);
        //Utils.setText(dlg , "DateEnd" , fDate);

        super.onInitMetadataDialog(dialog);
    }

}
