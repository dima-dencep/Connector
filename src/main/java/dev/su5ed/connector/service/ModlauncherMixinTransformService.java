package dev.su5ed.connector.service;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSpecBuilder;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.launch.MixinLaunchPluginLegacy;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.Constants;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

@SuppressWarnings("rawtypes")
public class ModlauncherMixinTransformService implements ITransformationService {
    private static final String NAME = "connectormixin";
    private static final MethodHandle START_METHOD;
    private static final MethodHandle PLUGIN_INIT_METHOD;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            START_METHOD = MethodHandles.privateLookupIn(MixinBootstrap.class, lookup).findStatic(MixinBootstrap.class, "start", MethodType.methodType(boolean.class));
            PLUGIN_INIT_METHOD = MethodHandles.privateLookupIn(MixinLaunchPluginLegacy.class, lookup).findVirtual(MixinLaunchPluginLegacy.class, "init", MethodType.methodType(void.class, IEnvironment.class, List.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArgumentAcceptingOptionSpec<String> mixinsArgument;
    private final List<String> commandLineMixins = new ArrayList<>();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
        this.mixinsArgument = argumentBuilder.apply("config", "a mixin config to load").withRequiredArg().ofType(String.class);
    }

    @Override
    public void argumentValues(OptionResult option) {
        this.commandLineMixins.addAll(option.values(this.mixinsArgument));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {
        ILaunchPluginService plugin = new ConnectorMixinLaunchPlugin();
        try {
            Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
            launchPluginsField.setAccessible(true);
            LaunchPluginHandler launchPluginHandler = (LaunchPluginHandler) launchPluginsField.get(Launcher.INSTANCE);
            Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>) pluginsField.get(launchPluginHandler);
            // Ew hacks
            plugins.put(plugin.name(), plugin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(IEnvironment environment) {
        Optional<ILaunchPluginService> plugin = environment.findLaunchPlugin(ConnectorMixinLaunchPlugin.NAME);
        if (plugin.isEmpty()) {
            throw new MixinInitialisationError("Mixin Launch Plugin Service could not be located");
        }

        ILaunchPluginService launchPlugin = plugin.get();
        if (launchPlugin instanceof ConnectorMixinLaunchPlugin connectorMixinLaunchPlugin) {
            try {
                START_METHOD.invoke();

                PLUGIN_INIT_METHOD.invoke(connectorMixinLaunchPlugin, environment, this.commandLineMixins);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            throw new MixinInitialisationError("Mixin Launch Plugin Service is present but not compatible");
        }
    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        if (!FMLEnvironment.production) {
            MixinEnvironment.getDefaultEnvironment().getRemappers().add(new MixinModlauncherRemapper());
        }
        return List.of();
    }

    @Override
    public List<ITransformer> transformers() {
        return List.of();
    }
}
