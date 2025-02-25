package com.itsschatten.itemeditor.utils;

public enum TagType {

    /**
     * For tags that are applied before parsing the component.
     */
    PRE_PARSE,

    /**
     * For tags that insert content. They may not close themselves, so some of their formatting may bleed.
     */
    INSERT,

    /**
     * For tags that insert content. They will not have any of their formatting bleed.
     */
    INSERT_CLOSED,

    /**
     * Stylization. This tag does not support arguments.
     */
    STYLE

}
