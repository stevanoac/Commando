package dev.stevanoac.commando.internal;

import dev.stevanoac.commando.help.HelpEntry;

import java.util.*;

/**
 * Trie / Prefix-tree node representing a token in a whitespace-separated command path.
 */
public class RouteNode {

    private final String token;
    private final RouteNode parent;
    private final Map<String, RouteNode> children = new HashMap<>();
    private final List<RouteNode> uniqueChildren = new ArrayList<>();
    private final List<String> aliases = new ArrayList<>();

    private RouteHandler handler;

    public RouteNode(String token, RouteNode parent) {
        this.token = token.toLowerCase();
        this.parent = parent;
    }

    public RouteNode getOrCreateChild(String childToken) {
        String lower = childToken.toLowerCase();
        RouteNode existing = children.get(lower);
        if (existing == null) {
            existing = new RouteNode(lower, this);
            children.put(lower, existing);
            uniqueChildren.add(existing);
        }
        return existing;
    }

    public void addAlias(String alias, RouteNode childNode) {
        String lower = alias.toLowerCase();
        children.put(lower, childNode);
        if (!childNode.aliases.contains(lower)) {
            childNode.aliases.add(lower);
        }
    }

    public RouteNode findChild(String childToken) {
        return children.get(childToken.toLowerCase());
    }

    public void collectHelpEntries(List<HelpEntry> list) {
        if (handler != null) {
            StringBuilder syntax = new StringBuilder(handler.getFullRoute());
            for (ParameterResolver resolver : handler.getAdapterResolvers()) {
                syntax.append(" <").append(resolver.getName()).append(">");
            }

            list.add(new HelpEntry(
                    syntax.toString(),
                    handler.getDescription(),
                    handler.getPermission(),
                    handler.isConsole()
            ));
        }

        for (RouteNode child : uniqueChildren) {
            child.collectHelpEntries(list);
        }
    }

    public String getToken() {
        return token;
    }

    public RouteNode getParent() {
        return parent;
    }

    public Map<String, RouteNode> getChildren() {
        return children;
    }

    public List<RouteNode> getUniqueChildren() {
        return uniqueChildren;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public RouteHandler getHandler() {
        return handler;
    }

    public void setHandler(RouteHandler handler) {
        this.handler = handler;
    }

    public boolean hasHandler() {
        return handler != null;
    }
}
