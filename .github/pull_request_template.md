[A clear and concise description of what the pull request is for along with a reference to the associated issue IDs, if they exist.]

## Checklist

* Base your changes on `2.x` branch if you are targeting Log4j 2; use `main` otherwise
* `./mvnw verify` succeeds (if it fails due to code formatting issues reported by Spotless, simply run `spotless:apply` and retry)
* Changes contain an entry file in the `src/changelog/.2.x.x` directory
* Tests for the changes are provided
* [Commits are signed](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits) (optional, but highly recommended)
