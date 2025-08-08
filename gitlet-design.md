# Gitlet Design Document

**Name**: Qiyue Hao

Get started in 7.25
- logic delegation
- what in main what not, if not then in where
- Main class should mostly be calling helper methods in the the Repository class
-  A Gitlet system is considered “initialized” in a particular location if it has a .gitlet directory there
- To exit your program immediately, you may call System.exit(0)
- serialization time cannot depend in any way on the total size of files that have been added, committed
- hashes for commits and hashes for blobs:hash in an extra word for each object that has one value for blobs and another for commits.

## Classes and Data Structures

### Commit

#### Fields

1. Message
2. Timestamp
3. Parent


### Class 2

#### Fields

1. Field 1
2. Field 2


## Algorithms

## Persistence

