package com.sivannsan.millifile;

import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.utility.FileUtility;
import com.sivannsan.foundation.Validate;
import com.sivannsan.millidata.MilliData;
import com.sivannsan.millidata.MilliDataParseException;
import com.sivannsan.millidata.MilliNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * TODO: Rule for filename
 */
public final class MilliFiles {
    private MilliFiles() {
    }

    /**
     * Load and also create if it does not exist.
     * The loaded MilliFile is considered as root.
     */
    @Nonnull
    public static MilliFile load(@Nonnull String filename) throws MilliFileLoadException {
        return load(new File(filename));
    }

    /**
     * Load and also create if it does not exist.
     * The loaded MilliFile is considered as root.
     */
    @Nonnull
    public static MilliFile load(@Nonnull File file) throws MilliFileLoadException {
        if (!file.exists()) {
            //TODO: If not exist yet, we can not know it is a directory or not
            if (file.isDirectory()) {
                if (file.getName().endsWith(".mll")) throw new MilliFileLoadException("A MilliCollection filename must NOT end with .mll!");
                file.mkdirs();
            } else {
                if (!file.getName().endsWith(".mll")) throw new MilliFileLoadException("A MilliDocument filename must end with .mll extension!");
                FileUtility.createFile(file);
                FileUtility.write(file, MilliNull.INSTANCE.toString());
            }
        }
        if (file.isDirectory()) {
            if (file.getName().endsWith(".mll")) throw new MilliFileLoadException("A MilliCollection filename must NOT end with .mll!");
            return new IMilliCollection(null, file);
        } else {
            if (!file.getName().endsWith(".mll")) throw new MilliFileLoadException("A MilliDocument filename must end with .mll extension!");
            return new IMilliDocument(null, file);
        }
    }

    private static abstract class IMilliFile implements MilliFile {
        protected final MilliCollection parent;
        @Nonnull
        protected final File file;

        protected IMilliFile(MilliCollection parent, @Nonnull File file) {
            this.parent = parent;
            this.file = Validate.nonnull(file);
        }

        @Override
        public MilliCollection getParent() {
            return parent;
        }

        @Override
        @Nonnull
        public File getFile() {
            return file;
        }

        @Override
        public boolean isMilliDocument() {
            return this instanceof MilliDocument;
        }

        @Override
        public boolean isMilliCollection() {
            return this instanceof MilliCollection;
        }

        @Override
        @Nonnull
        public final MilliDocument asMilliDocument() throws ClassCastException {
            if (isMilliDocument()) return (MilliDocument) this;
            throw new ClassCastException("Not a MilliDocument");
        }

        @Override
        @Nonnull
        public final MilliCollection asMilliCollection() throws ClassCastException {
            if (isMilliCollection()) return (MilliCollection) this;
            throw new ClassCastException("Not a MilliCollection");
        }

        @Override
        public void delete() {
            FileUtility.delete(file);
        }
    }

    private static final class IMilliDocument extends IMilliFile implements MilliDocument {
        private IMilliDocument(MilliCollection parent, @Nonnull File file) {
            super(parent, file);
        }

        @Override
        @Nonnull
        public MilliData getContent() throws MilliDataParseException {
            Validate.nonnull(file);
            if (!file.exists()) return MilliNull.INSTANCE;
            StringBuilder sb = new StringBuilder();
            try {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) sb.append(scanner.nextLine().trim());
                scanner.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return MilliData.Parser.parse(sb.toString());
        }

        @Override
        public void setContent(@Nonnull MilliData value) {
            setContent(value, 0);
        }

        @Override
        public void setContent(@Nonnull MilliData value, int indent) {
            try {
                String content = Validate.nonnull(value).toString(indent);
                Validate.nonnull(file);
                if (!file.exists() && !file.createNewFile()) return;
                FileWriter writer = new FileWriter(file);
                writer.write(content);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final class IMilliCollection extends IMilliFile implements MilliCollection {
        private IMilliCollection(MilliCollection parent, @Nonnull File file) {
            super(parent, file);
        }

        /**
         * Get all MilliDatabases from files that are compatible with MilliDatabase
         */
        @Override
        @Nonnull
        public List<MilliFile> getFiles() {
            List<MilliFile> list = new ArrayList<>();
            for (File f : file.listFiles()) {
                if (f.isDirectory()) list.add(new IMilliCollection(this, f));
                else if (f.getName().endsWith(".mll")) list.add(new IMilliDocument(this, f));
            }
            return list;
        }

        @Override
        @Nonnull
        public MilliDocument getDocument(@Nonnull String name) {
            if (!name.endsWith(".mll")) throw new IllegalArgumentException("A MilliDocument name must end with .mll extension!");
            File newFile = new File(file, name);
            if (newFile.isDirectory()) {
                FileUtility.delete(newFile);
            }
            if (!newFile.exists()) {
                FileUtility.createFile(newFile);
                FileUtility.write(newFile, MilliNull.INSTANCE.toString());
            }
            return new IMilliDocument(this, newFile);
        }

        @Override
        @Nonnull
        public MilliCollection getCollection(@Nonnull String name) {
            if (name.endsWith(".mll")) throw new IllegalArgumentException("A MilliDocument name must NOT end with .mll extension!");
            File newFile = new File(file, name);
            if (newFile.isFile()) {
                FileUtility.delete(newFile);
            }
            if (!newFile.exists()) {
                FileUtility.createDirectory(newFile);
            }
            return new IMilliCollection(this, newFile);
        }

        @Override
        @Nonnull
        public Iterator<MilliFile> iterator() {
            return getFiles().iterator();
        }
    }
}
