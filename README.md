
# log4j-good-bits

## Overview

**log4j-good-bits** are the bits of log4j 1.2.17 that aren't currently raising CVEs

As at 2022-07-07, this bits excluded are:

* SocketServer
* SMTPAppender
* JMSAppender
* JMSSink
* JDBCAppender
* chainsaw

## Have the package names changed ?

No.

## Has the maven artifactId:groupId changed ?

Yes.

The old log4j pom.xml dependency:
```
<dependency>
  <groupId>log4j</groupId>
  <artifactId>log4j</artifactId>
  <version>1.2.17</version>
</dependency>
```  

is now
```
<dependency>
  <groupId>com.randomnoun.log4j</groupId>
  <artifactId>log4j-good-bits</artifactId>
  <version>1.2.17-1</version>
</dependency>  
```  

## What else has changed ?

* Renamed the source files with CVEs (and their test sources) from "xxxx.java" to "xxxx.nocompile"
* Bumped the maven-compiler-plugin source and target versions to 1.6
* Modified the scm and distributionManagement sections in the pom.xml
* Updated the maven-javadoc-plugin section
* Removed the sun.jdk:tools dependency on the ant-run plugin
* Removed the maven-release-plugin section
* Removed the maven site sections


## So does this repository have all the tags and branches of the original svn repository ?

Hopefully. Here's the process I used to convert the original repository:

```
sudo apt-get install svn2git
mkdir log4j
cd log4j

# this mostly appears to work, but fails towards the end with the message
# 'command failed: git checkout "BRNACH_1_3_ABANDONED'
svn2git http://svn.apache.org/repos/asf/logging/log4j

# create missing tags and branches, from https://stackoverflow.com/a/45056412
git for-each-ref --format="%(refname:short)" refs/remotes/svn |
   sed 's#svn/##' | grep -v '^tags' |
      while read aBranch; do git branch $aBranch svn/$aBranch || exit 1; done
      
git for-each-ref --format="%(refname:short)" refs/remotes/svn/tags/ |
while read tag; do
    GIT_COMMITTER_DATE="`git log -1 --pretty=format:\"%ad\" \"$tag\"`" \
    GIT_COMMITTER_EMAIL="`git log -1 --pretty=format:\"%ce\" \"$tag\"`" \
    GIT_COMMITTER_NAME="`git log -1 --pretty=format:\"%cn\" \"$tag\"`" \
    git tag -a -m "`git for-each-ref --format=\"%(contents)\" \"$tag\"`" \
        "`echo \"$tag\" | sed 's#svn/tags/##'`" "$tag" || exit 1
done

git push -u origin --all
git push -u origin --tags
```

The original master branch is at `master-from-svn-conversion`.

## Licensing

log4j-good-bits is licensed under the Apache 2.0 license.
