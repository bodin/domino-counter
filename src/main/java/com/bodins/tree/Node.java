package com.bodins.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Node<T> {

    private final Path id;
    private T data;
    private Map<String, Node<T>> children = new HashMap<>();

    public Node(Path id) {
        this.id = id;
    }

    public Node(Path id, T data) {
        this.id = id;
        this.data = data;
    }

    public void visit(Visitor<T> v){
        v.visit(this.data);
        this.children.values().stream().forEach(c -> c.visit(v));
    }

    public Collection<Node<T>> getChildren() {
        return children.values();
    }

    public Node<T> find(Path p){
        Path child = p.after(this.id);
        Node next = this.children.get(child.first());
        if(next == null || next.id.equals(p)){
            return next;
        }else{
            return next.find(p);
        }
    }

    public Path getId() {
        return id;
    }
    public void add(Path p, T d){
        Path childPath = p.after(this.id);
        if(childPath == null) return;
        String childId = childPath.first();

        if(childPath.isRoot()){
            //my direct child
            if(this.children.containsKey(childId)){
                this.children.get(childId).data = d;
            }else{
                this.children.put(childId, new Node(p, d));
            }
        }else if(this.children.containsKey(childId)){
            // a child of an existing child
            this.children.get(childId).add(p, d);
        } else {
            // a new child
            Node child = new Node(new Path(this.id, childId));
            child.add(p, d);
            this.children.put(childId, child);
        }

    }
    public T getData() {
        return data;
    }

    public void setData(T contour) {
        this.data = contour;
    }

    public String toString(){
        return this.toStringInteneral("");
    }
    private String toStringInteneral(String padding){
        return padding + this.id.toString() + "\n" +
                this.children.values()
                        .stream()
                        .map(n -> n.toStringInteneral(padding + "  "))
                        .collect(Collectors.joining(""));
    }
}
