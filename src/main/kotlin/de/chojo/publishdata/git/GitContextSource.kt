package de.chojo.publishdata.git

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * Provides the [GitContext] of the build, from CI environment variables or else the local `git`.
 *
 * Being a [ValueSource], it is re-evaluated whenever the configuration cache is checked, so a new
 * commit, branch or tag invalidates a cached configuration.
 */
abstract class GitContextSource : ValueSource<GitContext, GitContextSource.Parameters> {

    /** Parameters of [GitContextSource]. */
    interface Parameters : ValueSourceParameters {
        /** A directory inside the working copy. */
        val workingDirectory: DirectoryProperty
    }

    @get:Inject
    abstract val exec: ExecOperations

    override fun obtain(): GitContext =
        CiEnvironment.detect(System::getenv)
            ?: LocalGit(::git).detect(public = System.getenv("PUBLIC_BUILD") == "true")

    private fun git(args: List<String>): String? {
        val output = ByteArrayOutputStream()
        val result = try {
            exec.exec {
                executable("git")
                args(args)
                workingDir(parameters.workingDirectory)
                standardOutput = output
                errorOutput = OutputStream.nullOutputStream()
                isIgnoreExitValue = true
            }
        } catch (e: GradleException) {
            return null
        }
        return output.toString().trim().takeIf { result.exitValue == 0 && it.isNotEmpty() }
    }
}
