**INSERT HERE** a clear and concise description of what the pull request is for along with a reference to the associated issue IDs, if they exist.

> [!IMPORTANT]  
> Base your changes on `2.x` branch if you are targeting Log4j 2; use `main` otherwise.

## Checklist

Before we can review and merge your changes, please go through the checklist below. If you're still working on some items, feel free to submit your pull request as a draft—our CI will help guide you through the remaining steps.

### ✅ Required checks

- [ ] **License**: I confirm that my changes are submitted under the [Apache License, Version 2.0](https://apache.org/licenses/LICENSE-2.0).
- [ ] **Commit signatures**: All commits are signed and verifiable. (See [GitHub Docs on Commit Signature Verification](https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification)).
- [ ] **Code formatting**: The code is formatted according to the project’s style guide.
  <details>
    <summary>How to check and fix formatting</summary>

    - To **check** formatting: `./mvnw spotless:check`
    - To **fix** formatting: `./mvnw spotless:apply`

    See [the build instructions](https://logging.apache.org/log4j/2.x/development.html#building) for details.
  </details>
- [ ] **Build & Test**: I verified that the project builds and all unit tests pass.
  <details>
    <summary>How to build the project</summary>

    Run: `./mvnw verify`

    See [the build instructions](https://logging.apache.org/log4j/2.x/development.html#building) for details.
  </details>

### 🧪 Tests (select one)

- [ ] I have added or updated tests to cover my changes.
- [ ] No additional tests are needed for this change.

### 📝 Changelog (select one)

- [ ] I added a changelog entry in `src/changelog/.2.x.x`. (See [Changelog Entry File Guide](https://logging.apache.org/log4j/tools/log4j-changelog.html#changelog-entry-file)).
- [ ] This is a trivial change and does not require a changelog entry.
