package de.miraculixx.bmm;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class BMMPluginLoader implements PluginLoader {
    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
    private static final RemoteRepository MAVEN_SNAPSHOT = new RemoteRepository.Builder("snapshot", "default", "https://s01.oss.sonatype.org/content/repositories/snapshots").build();

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {

        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(MAVEN_CENTRAL);
        resolver.addRepository(MAVEN_SNAPSHOT);

        Stream.of(
                "org.jetbrains.kotlin:kotlin-stdlib:1.9.23",
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",
                "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4",
                "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1",
                "dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.5.0-SNAPSHOT"
            )
            .map(artifact -> new Dependency(new DefaultArtifact(artifact), null))
            .forEach(resolver::addDependency);

        classpathBuilder.addLibrary(resolver);
    }
}