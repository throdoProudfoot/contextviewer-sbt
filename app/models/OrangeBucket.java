package models;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.*;
import play.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: throdo
 * Date: 21/05/13
 * Time: 14:51
 */
public class OrangeBucket {

    private final static String designDocName = "display";
    private String id;
    private String label;
    private CouchbaseClient couchbaseClient;
    private Map<String, OrangeDocument> orangeDocuments;
    private List<BucketView> bucketViews;

    public OrangeBucket(String id, String label, CouchbaseClient couchbaseClient) {
        this.id = id;
        this.label = label;
        this.couchbaseClient = couchbaseClient;
        this.orangeDocuments = new HashMap<String, OrangeDocument>();
    }

    public List<BucketView> getBucketViews() {
        return bucketViews;
    }

    public void setBucketViews(List<BucketView> bucketViews) {
        this.bucketViews = bucketViews;
    }

    public void fillBucketWithViews() throws Exception {
        // URL pour l'appel REST :
        // http://127.0.0.1:8091/pools/default/buckets/bucketName/ddocs
        // ex : http://127.0.0.1:8091/pools/default/buckets/beer-sample/ddocs

        DesignDocument designDocument = null;
        try {
            designDocument = this.couchbaseClient.getDesignDocument(designDocName);
            Logger.debug("Contenu du Design Document : " + designDocument.toJson());
        } catch (InvalidViewException e) {
            Logger.info("Impossible de trouver le DesignDocument 'display'. Il faut le créer !");

            createDesignDocument();
        }


    }

    private void createDesignDocument() throws Exception {
        DesignDocument designDocument;List<ViewDesign> views = new ArrayList<ViewDesign>();
        List<SpatialViewDesign> spviews = new ArrayList<SpatialViewDesign>();

        ViewDesign allKeys = new ViewDesign(
                "all_keys",
                "function (doc, meta) {\n" +
                        "  emit(meta.id, doc);\n" +
                        "}");

        views.add(allKeys);

        SpatialViewDesign spview = new SpatialViewDesign(
                "spatialfoo",
                "function(map) {}"
        );
        spviews.add(spview);

        designDocument = new DesignDocument(designDocName, views, spviews);

        if (!this.couchbaseClient.createDesignDoc(designDocument)) {
            throw new Exception("Impossible de créer le DesignDocument " + designDocName + "!");
        }
        ;

        // TODO améliorer ce code pourri. Il faut un certain temps pour que le DesignDocument soit créer. Système de Synchronisation à mettre en place.
        boolean foundDesignDoc = false;
        int cpt=0;
        while (!foundDesignDoc) {
            try {
                cpt++;
                this.couchbaseClient.getDesignDocument(designDocName);
                foundDesignDoc = true;
                Logger.debug("Impossible de créer le DesignDocument " + designDocName + "!");
            } catch (Exception e1) {
                Thread.sleep(500);
                if (cpt > 5) {
                    throw new Exception("Impossible de créer le DesignDocument " + designDocName + "!");
                }
            }
        }
    }

    public void fillBucketWithDocuments() {
        // Create connection if needed

        try {
            View view = this.couchbaseClient.getView("display", "all_documents");
            Query query = new Query();
            query.setIncludeDocs(true).setLimit(20);
            query.setStale(Stale.FALSE);
            ViewResponse result = this.couchbaseClient.query(view, query);

            for (ViewRow row : result) {
                String key = row.getKey(); // deal with the document/data
                String value = row.getValue(); // deal with the document/data

                OrangeDocument document = new OrangeDocument(key, value);
                this.orangeDocuments.put(key, document);

            }
        } catch (InvalidViewException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Map<String, OrangeDocument> getOrangeDocuments() {
        return orangeDocuments;
    }

    public void setOrangeDocuments(Map<String, OrangeDocument> orangeDocuments) {
        this.orangeDocuments = orangeDocuments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public CouchbaseClient getCouchbaseClient() {
        return couchbaseClient;
    }

    public void setCouchbaseClient(CouchbaseClient couchbaseClient) {
        this.couchbaseClient = couchbaseClient;
    }
}
