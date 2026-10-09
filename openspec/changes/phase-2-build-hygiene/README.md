# phase-2-build-hygiene

Aggregate the JaCoCo coverage gate across the composite build, add `-parameters` to
every Java compile, and wire test logging so a failing test prints the class and method
that ran. The OWASP dependency-check plugin landed in the 2.1.2 entry of every
changelog and is not part of this change.