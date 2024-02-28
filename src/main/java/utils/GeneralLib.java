package utils;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.IProcessType;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.evITAWeb.api.IDialog;
import com.ser.foldermanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GeneralLib {

    public static ISession session = null;
    public static class ClassIDs{
        public static final String QAWorkspace = "5e664e59-3c81-48dd-925e-4e731f12ca65";
    }
    public static class Databases{
        public static final String QAWorkspace = "D_QA";
    }
    public static class DescriptorLiterals{
        public static final String CompCode = "ORGCOMPANYDESCRIPTION";
	}
    public static class Descriptors{
        public static final String PatternDocNr = "PatternDocumentNo";
        public static final String PatternImpNr = "PatternImportNo";
        public static final String PatternDrfNo = "PatternDraftNo";
        public static final String PatternReqNo = "PatternRequestNo";
        public static final String CompCode = "OrgCompanyDescription";
        public static final String CatgCode = "ObjectCategoryName";
        public static final String TypeCode = "ObjectType";
        public static final String DocId = "ObjectDocID";
        public static final String DocNr = "ObjectNumber";
        public static final String RevNr = "ObjectNumberExternal";
        public static final String Status = "ObjectStatus";
        public static final String ReqLastId = "ReqLastId";
        public static final String ReqPrevId = "ReqPrevId";

    }
    public static ISession ses;
    private static final Logger log = LoggerFactory.getLogger(GeneralLib.class);
    public static void setSes(ISession ses) {
        GeneralLib.ses = ses;
    }

    public static boolean disconnectToFolder(IFolder folder, String rootName, String fold, IInformationObject pdoc) throws Exception {
        boolean add2Node = false;
        List<INode> nodes = folder.getNodesByName(rootName);
        if(nodes.isEmpty()){return false;}

        INodes root = (INodes) nodes.get(0).getChildNodes();
        INode fnod = root.getItemByName(fold);
        if(fnod == null) {return false;}

        int rIndex = -1;
        String pdocID = pdoc.getID();
        IElements nelements = fnod.getElements();
        for(int i=0;i<nelements.getCount2();i++) {
            IElement nelement = nelements.getItem2(i);
            String edocID = nelement.getLink();
            if(!Objects.equals(pdocID, edocID)){
                continue;
            }
            nelements.remove(i);
            rIndex = i;
        }
        if(rIndex<0) {return false;}

        folder.commit();
        return add2Node;
    }

    public static boolean hasDescriptor(IInformationObject object, String descName){
        IDescriptor[] descs = session.getDocumentServer().getDescriptorByName(descName, session);
        List<String> checkList = new ArrayList<>();
        for(IDescriptor ddsc : descs){checkList.add(ddsc.getId());}

        String[] descIds = new String[0];
        if(object instanceof IFolder){
            String classID = object.getClassID();
            IArchiveFolderClass folderClass = session.getDocumentServer().getArchiveFolderClass(classID , session);
            descIds = folderClass.getAssignedDescriptorIDs();
        } else if(object instanceof IDocument){
            IArchiveClass documentClass = ((IDocument) object).getArchiveClass();
            descIds = documentClass.getAssignedDescriptorIDs();
        } else if(object instanceof ITask){
            IProcessType processType = ((ITask) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        } else if(object instanceof IProcessInstance){
            IProcessType processType = ((IProcessInstance) object).getProcessType();
            descIds = processType.getAssignedDescriptorIDs();
        }

        List<String> descList = Arrays.asList(descIds);
        for(String dId : descList){if(checkList.contains(dId)){return true;}}
        return false;
    }
    public static String getQAReqId(IInformationObject qaws, String compCode, String rqId) throws Exception {
        String rtrn = (rqId == null ? "" : rqId.trim());
        if(!rtrn.isEmpty()) { return rtrn;}

        String cntPattern = "";
        if(hasDescriptor(qaws, Descriptors.PatternReqNo)){
            cntPattern = qaws.getDescriptorValue(Descriptors.PatternReqNo, String.class);
            cntPattern = (cntPattern == null ? "" : cntPattern).trim();
        }

        String reqType = "Document";
        if(!cntPattern.isEmpty()
                && !compCode.isEmpty()
                && !reqType.isEmpty()){
            String counterName = AutoText.init().with(qaws)
                    .param("Company", compCode)
                    .param("ReqType", reqType)
                    .run("QA.{Company}.Request.{ReqType}");

            NumberRange nr = new NumberRange();
            if(!nr.has(counterName)){
                nr.append(counterName, cntPattern, 0L);
            }

            nr.parameter("Company", compCode);
            nr.parameter("ReqType", reqType);
            rtrn = nr.increment(counterName, cntPattern);
        }
        return rtrn;
    }

    public static IDocument getQAWorkspace(String ccod)  {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(ClassIDs.QAWorkspace).append("'")
                .append(" AND ")
                .append(DescriptorLiterals.CompCode).append(" = '").append(ccod).append("'");
        String whereClause = builder.toString();
        System.out.println("Where Clause: " + whereClause);

        IInformationObject[] informationObjects = createQuery(new String[] {Databases.QAWorkspace} , whereClause , 1);
        if(informationObjects == null || informationObjects.length < 1) {return null;}
        return (IDocument) informationObjects[0];
    }
    public static IInformationObject[] createQuery(String[] dbNames , String whereClause , int maxHits){
        String[] databaseNames = dbNames;

        ISerClassFactory fac = session.getDocumentServer().getClassFactory();
        IQueryParameter que = fac.getQueryParameterInstance(
                session ,
                databaseNames ,
                fac.getExpressionInstance(whereClause) ,
                null,null);
        if(maxHits > 0) {
            que.setMaxHits(maxHits);
            que.setHitLimit(maxHits + 1);
            que.setHitLimitThreshold(maxHits + 1);
        }
        IDocumentHitList hits = que.getSession() != null? que.getSession().getDocumentServer().query(que, que.getSession()):null;
        if(hits == null) return null;
        else return hits.getInformationObjects();
    }
}
