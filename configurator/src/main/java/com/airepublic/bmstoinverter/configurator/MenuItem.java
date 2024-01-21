package com.airepublic.bmstoinverter.configurator;

public class MenuItem {
    private String displayName;
    private String dependency;

    public MenuItem(final String displayName, final String dependency) {
        setDisplayName(displayName);
        setDependency(dependency);
    }


    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }


    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }


    /**
     * @return the dependency
     */
    public String getDependency() {
        return dependency;
    }


    /**
     * @param dependency the dependency to set
     */
    public void setDependency(final String dependency) {
        this.dependency = dependency;
    }


    @Override
    public int hashCode() {
        return displayName.hashCode();
    }


    @Override
    public String toString() {
        return getDisplayName();
    }

}
