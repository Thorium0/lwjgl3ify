package me.eigenraven.lwjgl3ify.core;

import me.eigenraven.lwjgl3ify.Lwjgl3Aware;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class LwjglRedirectTransformer extends Remapper implements IClassTransformer {
    int remaps = 0, calls = 0;

    public static LwjglRedirectTransformer activeInstance = null;

    public LwjglRedirectTransformer() {
        // Only use the last constructed transformer
        activeInstance = this;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (this != activeInstance) {
            return basicClass;
        }
        if (basicClass == null) {
            return null;
        }
        if (name.contains("lwjgl3ify")) {
            return basicClass;
        }
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(0);
        ClassVisitor visitor = new RemappingClassAdapter(writer, this) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals(Type.getDescriptor(Lwjgl3Aware.class))) {
                    throw new Lwjgl3AwareException();
                }
                return super.visitAnnotation(desc, visible);
            }
        };

        try {
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        } catch (Lwjgl3AwareException e) {
            return basicClass;
        } catch (Exception e) {
            Lwjgl3ifyCoremod.LOGGER.warn("Couldn't remap class {}", transformedName, e);
            return basicClass;
        }

        return writer.toByteArray();
    }

    final String[] fromPrefixes = new String[] {
        "org/lwjgl/", "paulscode/sound/libraries/", "javax/xml/bind/",
    };

    final String[] toPrefixes = new String[] {
        "org/lwjglx/", "me/eigenraven/lwjgl3ify/paulscode/sound/libraries/", "jakarta/xml/bind/",
    };

    @Override
    public String map(String typeName) {
        if (typeName == null) {
            return null;
        }
        calls++;
        for (int pfx = 0; pfx < fromPrefixes.length; pfx++) {
            if (typeName.startsWith(fromPrefixes[pfx])) {
                remaps++;
                final String newName = toPrefixes[pfx] + typeName.substring(fromPrefixes[pfx].length());
                return newName;
            }
        }

        return typeName;
    }

    private class Lwjgl3AwareException extends RuntimeException {}
}