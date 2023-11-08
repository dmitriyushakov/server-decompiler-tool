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

## What if you have a lot of classes in solution
Sometimes you would like to scan very big application. By default, the tool work with in memory index registry and Java Heap going to be exhausted as soon as fast the tool can scan classes. There few recommendations that you can use in that cases.

### Make larger Java Heap limit
Most obvious solution is just to increase maximum Java Heap size by JVM -Xmx flag like in this way:
```bash
java -Xmx8g -jar server-decompiler-tool.jar "/path/to/app/libs"
```

### Use file based index registry
You can switch to file based index registry. It would take more time to parse classes than it could be taken with in memory index registry. But in this case you are not limited by Java Heap to persist index registry.
Use either `--index temp-file` or `--index file`. If `temp-file` index would be used then index files will be deleted after application finish. 
```bash
java -jar server-decompiler-tool.jar --index file "/path/to/app/libs"
```

## Features to implement in future releases
* Ability to exclude part of subjects from index + lazy index. It would allow to reduce Java Heap size for in memory index and increase speed of creation for file based index.
* Maybe some other things. I didn't decide yet.
