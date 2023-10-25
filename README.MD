# Server decompiler tool
This utility allow to scan directories with compiled Java classes, JAR files. You can use there one of three decompilers right in web interface:
* [JD Core](https://java-decompiler.github.io/)
* [CFR](https://www.benf.org/other/cfr/)
* [Procyon](https://github.com/mstrobel/procyon)

Web interface allow to not download solution from server side. You can put JAR file with tool directly from server and launch there.

## Main features
* Web user interface with navigation in code. You can follow through links in syntax highlight and search related objects in context menu (by right mouse button click).
* Three decompilers - JD Core, CFR, Procyon
* All classes placed in packages tree - you don't have to search JAR file where class is located.
* Search classes by name.
* Fast solution indexation.

## Quick start guide
1. Download build from [releases page](https://github.com/dmitriyushakov/server-decompiler-tool/releases) or build by `gradle jar` task. It would make fat JAR build.
2. Run application
```bash
java -jar server-decompiler-tool.jar "<class path 1>;<class path 2>;...;<class path N>"
```
If class path not specified, current working directory would be scanned then. 
3. Open web interface (default 8080 port).
4. Find class in the tree on left side or in search (can be opened by button with magnifier icon).

## Features to implement in future releases
* File based index. Current implementation support only in memory index. It could require large heap size to scan solution.
* Maybe some other things. I didn't decide yet.