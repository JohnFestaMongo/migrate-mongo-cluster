package com.mongodb.migratecluster.observables;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.migratecluster.commandline.Resource;
import com.mongodb.migratecluster.model.DocumentsBatch;
import com.mongodb.migratecluster.observers.BaseDocumentWriter;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File: DocumentWriter
 * Author: shyam.arjarapu
 * Date: 4/26/17 5:02 AM
 * Description:
 */
public class DocumentWriter extends Observable<DocumentsBatch> {
    private final static Logger logger = LoggerFactory.getLogger(DocumentWriter.class);
    private final DocumentReader documentReader;
    private final MongoClient client;
    private final Resource resource;

    public DocumentWriter(MongoClient client, DocumentReader documentReader) {
        this.client = client;
        this.documentReader = documentReader;
        this.resource = documentReader.getResource();
    }

    @Override
    protected void subscribeActual(Observer<? super DocumentsBatch> observer) {
        AtomicInteger documentCountTracker = new AtomicInteger(0);
        AtomicInteger batchIdTracker = new AtomicInteger(0);

        Observable<DocumentsBatch> observable = this.documentReader
            .flatMap(new Function<DocumentsBatch, ObservableSource<DocumentsBatch>>() {
                @Override
                public ObservableSource<DocumentsBatch> apply(DocumentsBatch batch) throws Exception {
                    // you got entire documents in here
                    // go save them to the target database in parallel
                    // NOTE: Running on the .subscribeOn(Schedulers.io()) threads makes the order quite random.
                    return Observable
                            .just(batch.getDocuments())
                            //.subscribeOn(Schedulers.io())
                            .map(documents -> {
                                MongoCollection<Document> collection = getMongoCollection();
                                collection.insertMany(documents);

                                // TODO: Update the lastDocument entry in oplog database
                                String message = String.format("Batch %s. Inserted %d documents into target collection: %s",
                                        batchIdTracker.addAndGet(1), documents.size(), resource.getNamespace());
                                logger.info(message);
                                documentCountTracker.addAndGet(documents.size());

                                observer.onNext(batch);
                                return batch;
                            });
                }
            });

        observable.blockingSubscribe();

        logger.info("Completed writing {} documents to Resource: {}", documentCountTracker.get(), this.resource);
        observer.onComplete();
    }

    private MongoCollection<Document> getMongoCollection() {
        return BaseDocumentWriter.getInstance(client).getMongoCollection(
                resource.getNamespace(),
                resource.getDatabase(),
                resource.getCollection());
    }


}
