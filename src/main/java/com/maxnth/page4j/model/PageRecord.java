package com.maxnth.page4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a builder ({@code XBuilder}) and {@code with…} methods ({@code XBuilder.With}) for a
 * model record. Lists left unset are empty, and {@code addX} adds a single list element.
 */
@RecordBuilder.Template(
    options =
        @RecordBuilder.Options(
            useImmutableCollections = true,
            addSingleItemCollectionBuilders = true,
            addStaticBuilder = false,
            nullableAnnotationClass = "org.jspecify.annotations.Nullable"))
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
@interface PageRecord {}
