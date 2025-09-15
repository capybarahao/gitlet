# Gitlet Design Document

**Name**: Qiyue Hao

Get started in 7.25
- logic delegation
- what in main what not, if not then in where
- Main class should mostly be calling helper methods in the the Repository class
-  A Gitlet system is considered “initialized” in a particular location if it has a .gitlet directory there
- serialization time cannot depend in any way on the total size of files that have been added, committed
- 读spec serialization 部分 关于 runtime map between these strings and the runtime objects they refer to
- hashes for commits and hashes for blobs:hash in an extra word for each object that has one value for blobs and another for commits.
- Master / other branch name = some commit Sha1, also Head = some commit Sha1, where to store this info?
- file separator in Win / MacOS issue. Don't hardcode / or \ !! read "Things to avoid" in spec
- Be careful using a HashMap when serializing! The order of things within the HashMap is non-deterministic.
   The solution is to use a TreeMap which will always have the same order

- choice of data structure for storing file contents: not assume file as texts/strings but binary files. store them as byte[] instead of string.
- for example. stageForAdd should be String / byte[] map, instead of string/string one.
## Classes and Data Structures

### Commit

#### Fields

1. Message
2. Timestamp
3. ParentA & ParentB
4. Treemap fileToBlob
   a mapping of file names "wug.txt" to blob references "d12da..."


### Class 2

#### Fields

1. Field 1
2. Field 2


## Algorithms

## Persistence

