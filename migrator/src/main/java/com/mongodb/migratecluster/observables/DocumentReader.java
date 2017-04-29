package com.mongodb.migratecluster.observables;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.migratecluster.commandline.Resource;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File: DocumentReader
 * Author: shyam.arjarapu
 * Date: 4/26/17 4:48 AM
 * Description:
 */
public class DocumentReader  extends Observable<List<ResourceDocument>> {
    final static Logger logger = LoggerFactory.getLogger(DocumentReader.class);
    private final Resource resource;
    private  MongoCollection<Document> collection;

    public DocumentReader(MongoClient client, Resource resource) {
        this.resource = resource;
        this.collection = client.getDatabase(resource.getDatabase()).getCollection(resource.getCollection());
    }

    // TODO: Remove this code
    public DocumentReader(MongoCollection<Document> collection, Resource resource) {
        this.collection = collection;
        this.resource = resource;
    }

    @Override
    protected void subscribeActual(Observer<? super List<ResourceDocument>> observer) {
        Observable<Object> observable = new DocumentIdReader(collection, resource);
        AtomicInteger docsCount = new AtomicInteger(0);

        observable
                .buffer(1000)
                .flatMap(new Function<List<Object>, Observable<List<ResourceDocument>>>() {
                    @Override
                    public Observable<List<ResourceDocument>> apply(List<Object> ids) throws Exception {
                        return new DocumentsObservable(collection, getResource(), ids.toArray())
                                .subscribeOn(Schedulers.io());
                    }
                })
                .blockingSubscribe(k -> {
                    logger.info("reader for resource: {} got {} documents; so far read total {} documents in this run.",
                            this.resource.getNamespace(),  k.size(), docsCount.addAndGet(k.size()));
                    observer.onNext(k);
                });
        // NOTE: by not blocking here, there is possibility of missing last set in the buffer
        observable.blockingLast();
        observer.onComplete();
        logger.info("reader for resource: {} completed. total documents read: {}",
                this.resource.getNamespace(),  docsCount);
    }

    public Resource getResource() {
        return resource;
    }
}