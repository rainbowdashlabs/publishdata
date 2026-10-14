package de.chojo.publishdata.git

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocalGitTest {
    @Test
    fun `reads branch tags and commit`() {
        val outputs = mapOf(
            "symbolic-ref" to "main",
            "tag" to "v1.1.0\nv1.0.0\n",
            "rev-parse" to "abcdef",
        )
        val context = LocalGit { args -> outputs[args.first()] }.detect(public = true)
        assertThat(context).isEqualTo(GitContext("main", listOf("v1.1.0", "v1.0.0"), "abcdef", Ci.LOCAL, false, true))
    }

    @Test
    fun `no repository gives an empty context`() {
        val context = LocalGit { null }.detect(public = false)
        assertThat(context).isEqualTo(GitContext(null, emptyList(), null, Ci.LOCAL, false, false))
    }
}
