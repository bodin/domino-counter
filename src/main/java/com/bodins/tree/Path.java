package com.bodins.tree;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Path {
    private final String[] path;

    public Path(Path p, String id) {
        this.path = new String[p.path.length + 1];
        System.arraycopy(p.path, 0, this.path, 0, p.path.length);
        this.path[p.path.length] = id;
    }
    public Path(String... path) {
        this.path = path;
    }
    public static Path of(String ... paths){
        return new Path(paths);
    }

    /**
     *
     * @return true if this path only has one segment
     */
    public boolean isRoot(){
        return this.path.length == 1;
    }

    /**
     *
     * @return Returns the first part of the path (A|B|C -> A)
     */
    public String first(){
        if(this.path.length <= 0) return null;
        return this.path[0];
    }

    public Path after(Path p){
        int i;
        for(i = 0; i < this.path.length && i < p.path.length; i++){
            if(!this.path[i].equals(p.path[i])) break;
        }
        if(i >= this.path.length) return null;
        return new Path(Arrays.copyOfRange(this.path, i, this.path.length));
    }

    public String toKey(){
        return Arrays.stream(this.path).collect(Collectors.joining("|"));
    }

    /**
     *
     * @return The path to the parent Path.  (A|B|C -> A|B)
     */
    public Path getParent(){
        if(this.isRoot()) return null;
        return new Path(Arrays.copyOfRange(this.path, 0, this.path.length-1));
    }

    @Override
    public String toString() {
        return "Path{" +
                "path=" + Arrays.toString(path) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path1 = (Path) o;
        return Arrays.equals(path, path1.path);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(path);
    }
}
