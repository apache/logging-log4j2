<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<!--
This looks like it was generated, but it was actually modified from the
CONTRIBUTING.md file from Apache Commons Lang.
-->
# Contributing to Apache Log4j 2

You have found a bug or you have an idea for a cool new feature? Contributing code is a great way to give something back to
the open source community. Before you dig right into the code there are a few guidelines that we need contributors to
follow so that we can have a chance of keeping on top of things.

## Getting Started

+ Make sure you have a [GitHub account](https://github.com/join).
+ If you're planning to implement a new feature it makes sense to discuss your changes on the [dev list](https://logging.apache.org/log4j/2.x/mail-lists.html) first. This way you can make sure you're not wasting your time on something that isn't considered to be in Apache Log4j's scope.
+ Submit a ticket for your issue, assuming one does not already exist.
  + Clearly describe the issue including steps to reproduce when it is a bug.
  + Make sure you fill in the earliest version that you know has the issue.
+ Fork the repository on GitHub.

## Making Changes

+ Create a topic branch from where you want to base your work (this is usually the `2.x` branch).
+ Make commits of logical units.
+ Respect the original code style:
  + Only use spaces for indentation.
  + Create minimal diffs - disable on save actions like reformat source code or organize imports. If you feel the source code should be reformatted create a separate PR for this change.
  + Check for unnecessary whitespace with git diff --check before committing.
+ Make sure your commit messages are in the proper format. Your commit message should contain the associated issue ID.
+ Make sure you have added the necessary tests for your changes.
+ Run all the tests with `mvn clean verify` to assure nothing else was accidentally broken.

## Making Trivial Changes

For changes of a trivial nature to comments and documentation, it is not always necessary to create a new ticket.
In this case, it is appropriate to start the first line of a commit with '(doc)' instead of a ticket number.

## Submitting Changes

+ Sign the [Contributor License Agreement][cla] if you haven't already.
+ Push your changes to a topic branch in your fork of the repository.
+ Submit a pull request to the repository in the apache organization.
+ Update your issue and include a link to the pull request in the ticket.

## Additional Resources

+ [Project Guidelines](https://logging.apache.org/log4j/2.x/guidelines.html)
+ [Code Style Guide](https://logging.apache.org/log4j/2.x/javastyle.html)
+ [Apache Log4j 2 Issue Tracker](https://github.com/apache/logging-log4j2/issues)
+ [Contributor License Agreement][cla]
+ [General GitHub documentation](https://docs.github.com/)
+ [GitHub pull request documentation](https://docs.github.com/en/pull-requests)

[cla]:https://www.apache.org/licenses/#clas
