// Single source of truth for the JaCoCo measured class set.
//
// Applied by sky.jacoco-conventions for every module and by the aggregate report and
// gate in the root build. Keeping one list is what stops the report and the gate from
// enforcing different class sets.
object CoverageFilters {
    val exclusions: List<String> = listOf(
        "**/dto/**",
        "**/config/**",
        "**/*Application.class",
        "**/Constants.class"
    )
}
