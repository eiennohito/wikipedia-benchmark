/**
 * @author  Alon Eirew
 */

package wiki.data;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import wiki.elastic.ElasticDocCreateListener;
import wiki.elastic.IElasticAPI;
import wiki.utils.WikiToElasticConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ElasticPageHandler implements IPageHandler {

    private final IElasticAPI elasticApi;
    private final ActionListener<BulkResponse> listener;
    private final String indexName;
    private final String docType;
    private final int bulkSize;

    private int totalIdsCommitted = 0;
    private Object lock = new Object();

    private final List<WikiParsedPage> pages = new ArrayList<>();

    public ElasticPageHandler(IElasticAPI elasticApi, ActionListener<BulkResponse> listener, WikiToElasticConfiguration config) {
        this.elasticApi = elasticApi;
        this.listener = listener;
        this.indexName = config.getIndexName();
        this.docType = config.getDocType();
        this.bulkSize = config.getInsertBulkSize();
    }

    @Override
    public void addPage(WikiParsedPage page) {
        synchronized (lock) {
            if (page != null) {
                pages.add(page);
                if (this.pages.size() == this.bulkSize) {
                    flush();
                }
            }
        }
    }

    @Override
    public void flush() {
        synchronized (lock) {
            if (this.pages != null && this.pages.size() > 0) {
                List<WikiParsedPage> copyPages = new ArrayList<>(this.pages);
                totalIdsCommitted += this.pages.size();
                this.pages.clear();
                elasticApi.addBulkAsnc(this.listener, this.indexName, this.docType, copyPages);
            }
        }
    }

    @Override
    public void flushRemains() {
        if (this.pages != null) {
            List<WikiParsedPage> copyPages = new ArrayList<>(this.pages);
            totalIdsCommitted += this.pages.size();
            this.pages.clear();
            for(WikiParsedPage page : copyPages) {
                elasticApi.addDoc(this.indexName, this.docType, page);
            }
        }
    }

    public int getTotalIdsCommitted() {
        return totalIdsCommitted;
    }

    public int getPagesQueueSize() {
        return this.pages.size();
    }
}
