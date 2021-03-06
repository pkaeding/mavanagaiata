/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2011-2013, Sebastian Staudt
 */

package com.github.koraktor.mavanagaiata.mojo;

import java.io.File;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.github.koraktor.mavanagaiata.git.GitRepository;
import com.github.koraktor.mavanagaiata.git.GitRepositoryException;
import com.github.koraktor.mavanagaiata.git.jgit.JGitRepository;

/**
 * This abstract Mojo implements initializing a JGit Repository and provides
 * this Repository instance to subclasses.
 *
 * @author Sebastian Staudt
 * @see GitRepository
 * @since 0.1.0
 */
public abstract class AbstractGitMojo extends AbstractMojo {

    /**
     * The date format to use for various dates
     *
     * @parameter property="mavanagaiata.dateFormat"
     *            default-value="MM/dd/yyyy hh:mm a Z"
     */
    protected String dateFormat;

    /**
     * The working tree of the Git repository.
     * <br>
     * If there is only one project inside the Git repository this is probably
     * <code>${project.basedir}</code> (default).
     * <br>
     * <strong>Note:</strong> The <code>GIT_DIR</code> can be found
     * automatically even if this is not the real working tree but one of its
     * subdirectories. But Mavanagaiata cannot determine the state of the
     * working tree (e.g. for the dirty flag) if this is not set correctly.
     *
     * @parameter property="mavanagaiata.baseDir"
     *            default-value="${project.basedir}"
     */
    protected File baseDir;

    /**
     * The flag to append to refs if there are changes in the index or working
     * tree
     *
     * @parameter property="mavanagaiata.dirtyFlag"
     *            default-value = "-dirty"
     * @since 0.4.0
     */
    protected String dirtyFlag;

    /**
     * Specifies if the dirty flag should also be appended if there are
     * untracked files
     * <br>
     * If <code>false</code> only modified that are already known to Git will
     * cause the dirty flag to be appended.
     *
     * @parameter property="mavanagaiata.dirtyIgnoreUntracked"
     *            default-value="false"
     * @since 0.5.0
     */
    protected boolean dirtyIgnoreUntracked;

    /**
     * The <code>GIT_DIR</code> path of the Git repository
     * <br>
     * <strong>Warning:</strong> Do not set this when you don't have a good
     * reason to do so. The <code>GIT_DIR</code> can be found automatically if
     * your project resides somewhere in a usual Git repository.
     *
     * @parameter property="mavanagaiata.gitDir"
     */
    protected File gitDir;

    /**
     * The commit or ref to use as starting point for operations
     *
     * @parameter property="mavanagaiata.head"
     *            default-value="HEAD"
     */
    protected String head;

    /**
     * Skip the plugin execution
     *
     * @parameter property="mavanagaiata.skip"
     *            default-value="false"
     * @since 0.5.0
     */
    protected boolean skip = false;

    /**
     * Skip the plugin execution if not inside a Git repository
     *
     * @parameter property="mavanagaiata.skipNoGit"
     *            default-value="false"
     * @since 0.5.0
     */
    protected boolean skipNoGit = false;

    /**
     * The Maven project
     *
     * @parameter property="project"
     * @readonly
     */
    protected MavenProject project;

    /**
     * The prefixes to prepend to property keys
     *
     * @parameter
     */
    protected String[] propertyPrefixes = { "mavanagaiata", "mvngit" };

    protected GitRepository repository;

    /**
     * Generic execution sequence for a Mavanagaiata mojo
     * <p>
     * Will initialize any needed resources, run the actual mojo code and
     * cleanup afterwards.
     *
     * @see #cleanup
     * @see #init
     * @see #run
     * @throws MojoExecutionException
     */
    public final void execute() throws MojoExecutionException {
        if (!this.skip) {
            boolean init = false;
            try {
                init = this.init();

                if (init) {
                    this.run();
                }
            } finally {
                if (init) {
                    this.cleanup();
                }
            }
        }
    }

    /**
     * Saves a property with the given name into the project's properties
     *
     * The value will be stored two times – with "mavanagaiata" and "mvngit" as
     * a prefix.
     *
     * @param name The property name
     * @param value The value of the property
     */
    protected void addProperty(String name, String value) {
        Properties properties = this.project.getProperties();

        for(String prefix : this.propertyPrefixes) {
            properties.put(prefix + "." + name, value);
        }
    }

    /**
     * Closes the JGit repository object
     *
     * @see GitRepository#close
     */
    protected void cleanup() {
        if (this.repository != null) {
            this.repository.close();
            this.repository = null;
        }
    }

    /**
     * Generic initialization for all Mavanagaiata mojos
     * <p>
     * This will initialize the JGit repository instance for further usage by
     * the mojo.
     *
     * @throws MojoExecutionException if the repository cannot be initialized
     */
    protected boolean init() throws MojoExecutionException {
        try {
            this.initRepository();
            return true;
        } catch (GitRepositoryException e) {
            if (this.skipNoGit) {
                return false;
            }
            throw new MojoExecutionException("Unable to initialize Mojo", e);
        }
    }

    /**
     * Initializes a JGit Repository object for further reference
     *
     * @see GitRepository
     * @throws GitRepositoryException if retrieving information from the Git
     *         repository fails
     */
    protected void initRepository()
            throws GitRepositoryException {
        this.repository = new JGitRepository(this.baseDir, this.gitDir);
        this.repository.check();
        this.repository.setHeadRef(this.head);
    }

    /**
     * The actual implementation of the mojo
     * <p>
     * This is called internally by {@link #init}.
     */
    protected abstract void run() throws MojoExecutionException;

}
