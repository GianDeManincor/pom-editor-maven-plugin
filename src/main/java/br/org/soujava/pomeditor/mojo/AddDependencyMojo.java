/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.org.soujava.pomeditor;

import br.org.soujava.pomeditor.adddep.Dependency;
import br.org.soujava.pomeditor.control.AddDependency;
import br.org.soujava.pomeditor.control.PomChange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mojo responsible to add a given dependency to a target POM
 * if such dependency is not declared
 * or the given dependency's version is greater than the existent at target POM
 */
@Mojo(name = "add-dep")
public class AddDependencyMojo extends AbstractMojo {

    @Parameter(property = "gav")
    String gav;
    @Parameter(property = "type")
    String type;
    @Parameter(property = "classifier")
    String classifier;
    @Parameter(property = "scope")
    String scope;
    @Parameter(property = "pom", defaultValue = "pom.xml")
    String pom = "pom.xml";

    BiConsumer<Path, Dependency> addDependencyCommand;

    Function<Path, Boolean> backupFunction;

    Consumer<Path> rollbackFunction;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Path pomFile = Paths.get(pom);
        Dependency dependency = buildDependency();
        try {
            getLog().info(String.format("trying to add the dependency: %s to the \"%s\" file...", pomFile));

            transactionFor(pomFile)
                    .execute(() -> Optional
                            .ofNullable(this.addDependencyCommand)
                            .orElse(AddDependency::execute)
                            .accept(pomFile, dependency));

            getLog().info(String.format("added the dependency: %s to the \"%s\" file.", dependency, pomFile));
        } catch (Throwable ex) {
            throw new MojoFailureException(String.format("cannot add the dependency: %s to the \"%s\" file: %s",
                    dependency,
                    pomFile,
                    ex.getMessage()), ex);
        }
    }

    private PomChange transactionFor(Path pomFile) {
        return PomChange
                .builder()
                .withLogger(getLog()::info)
                .withPom(pomFile)
                .withBackupFunction(backupFunction)
                .withRollbackFunction(rollbackFunction)
                .build();
    }

    private Dependency buildDependency() throws MojoExecutionException {
        try {
            return Dependency
                    .ofGav(gav)
                    .setType(type)
                    .setClassifier(classifier)
                    .setScope(scope)
                    .build();
        } catch (RuntimeException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }


}