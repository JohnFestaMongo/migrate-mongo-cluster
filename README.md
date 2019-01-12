# Overview

## About the application

The migrate-mongo-cluster is an application to help you migrate the data from one server to the other. The objective of this application is to help you acheive live migration of the data from source database to target database. This application comes handy especially, when you are in shared cluster and want to change the shard key without unsharding and resharding. 

## What / How it does

From technical stand point of view, the application reads data document by document from the source database and writes them into the target database. The application also tails the oplog and reapply them on target once it copied all data. 

## Word of caution

Ideally, one should be using a backup of existing database, restore it to the server were you wanted to migrate, let the oplog catchup and re-elect the new server as primary. If for whatever reason, you cannot acheive the above recommended approach, you may use this application to do the migration, at your own risk.

## Assumptions

### Target shard keys are precreated

While migrating the data from source to target, it is assumed that new shard key of your interest is precreated at target before beginning the migration

## How to run the application

### Download from git

```bash
git clone git@github.com:sarjarapu/migrate-mongo-cluster.git
```

## Build using Maven

```bash
cd migrate-mongo-cluster/migrator
mvn clean compile package
```

## Help instructions

```bash
java -jar target/migrate-mongo-cluster-1.0-SNAPSHOT-jar-with-dependencies.jar -h

usage: migratecluster [-c <arg>] [-d] [-h] [-o <arg>] [-s <arg>] [-t <arg>]  
 -c,--config <arg>   configuration file for migration  
 -d,--drop           drop target collections before copying  
 -h,--help           print this message  
 -o,--oplog <arg>    oplog store connection string  
 -s,--source <arg>   source cluster connection string  
 -t,--target <arg>   target cluster connection string  
 -m,--mode <arg>     migration mode. Supported modes: oplogOnly
```

## Run the application using sample migration

```bash
java -jar target/migrate-mongo-cluster-1.0-SNAPSHOT-jar-with-dependencies.jar -c ../sample/sample-migration.conf
```

# Features to be build into program

Below are the list of features that I thought of incorporating into the application.

- [x] Get databases, collections and docs
- [x] Save the documents onto target server
- [x] Reactive Programming
- [x] Buffered read / Bulk write 
- [x] Multithreading - Read full documents in a different thread
- [x] Multithreading - Write full documents in a different thread
- [x] Drop database / collection before inserting
- [x] Oplog tail for each replicaSet 
- [x] Continuation from where we left off
- [x] Error handling, duplicate key, etc
- [x] Use connection string with all the members in replicaset
- [x] Retry logic when the primary is down
- [x] Read preference - secondary from source?
- [ ] Status Database to keep track of progress
- [ ] API to expose status of migrators from database
- [ ] Runtime injection of the log level
- [x] While copying find the id and continue where you left off
- [ ] Move the gapWatcher out of the oplogMigrator
- [ ] Apply the oplogs in bulk operations


## Random notes

-  I think oplog tail should make entry if not already exists and start saving tail to oplog tail db this will help in scenarios when source oplog headroom is small compared to time it takes to populate all the historical data. for simplicity i assume oplog is big enough for multiple days if an oplog entry already exists then wait till all the copy process is done begin the oplog tail apply operations only after copy process is completed.