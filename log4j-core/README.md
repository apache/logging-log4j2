# Building Log4j Core

Log4j 2 supports the Java Platform Module System. JPMS has requirements that conflict with several things 
Log4j also tries to support:
1. OSGi - OSGi frameworks are not packaged as OSGi modules. Including an OSGi implementation will cause
compiler errors while resolving the JPMS module inforation.
2. Garbage Free - The Google tool Log4j uses to verify that Log4j core is garbage free violates JPMS rules. The test 
compilations fail when it is included as a dependency.
3. Compiler bugs - When compiling with module-info.java included the classes in the appender, layout, and filter 
directories get "duplicate class" errors. For some reason these directory names are being interpreted as starting 
with upper case letters even though they are not. For some reason the compiler is showing an error 
that the class cannot be found even though it is being generated. See
   [JDK-8265826](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8265826).
4. Test classes that are used by other modules - Several test classes are used by other log4j modules and need
to be passed to them. This requires these classes exist in a package that is not used in log4j-core.
5. Test classes used by log4j-core must use the same package space to be able to access some methods in the classes 
being tested.
6. Once Java has compiled the main module with a module-info.java all test compiles also require one. Likewise,
a test compile with a module-info.java is not allowed if the main compile doesn't have one.
   
For these reasons the build will need to be processed as follows:
1. Move the Garbage Free tests to their own module. This will require copying all the test resources.
1. Compile all the main classes except module-info.java with the Plugin preprocessor.
1. Compile the main module-info.java.  
1. Compile the test classes used by other modules with module-info.java and with the plugin preprocessor.
1. Package these test classes in a test jar.
1. Delete the module-info and generated source for the test classes.
1. Move the main module-info to a temp location.   
1. Compile the unit test classes without module-info.java.
1. Move the main module-info back to the classes directory.   
1. Compile module-info.java for unit tests.
1. Run the unit tests.
1. Create the main jar if the unit tests pass.

Once the JDK bug is fixed this process can be simplified quite a bit since the components will all be able to be 
compiled once with the module-info.java file.
   