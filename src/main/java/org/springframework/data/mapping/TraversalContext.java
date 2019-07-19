/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A context object for lookups of values for {@link PersistentPropertyPaths} via a {@link PersistentPropertyAccessor}.
 * It allows to register functions to post-process the objects returned for a particular property, so that the
 * subsequent traversal would rather use the processed object. This is especially helpful if you need to traverse paths
 * that contain {@link Collection}s and {@link Map} that usually need indices and keys to reasonably traverse nested
 * properties.
 *
 * @author Oliver Drotbohm
 */
public class TraversalContext {

	private Map<PersistentProperty<?>, Function<Object, Object>> handlers = new HashMap<>();

	public TraversalContext registerHandler(PersistentProperty<?> property, Function<Object, Object> handler) {
		handlers.put(property, handler);
		return this;
	}

	@SuppressWarnings("unchecked")
	public TraversalContext registerCollectionHandler(PersistentProperty<?> property,
			Function<? super Collection<?>, Object> handler) {
		return registerHandler(property, Collection.class, (Function<Object, Object>) handler);
	}

	@SuppressWarnings("unchecked")
	public TraversalContext registerMapHandler(PersistentProperty<?> property,
			Function<? super Map<?, ?>, Object> handler) {
		return registerHandler(property, Map.class, (Function<Object, Object>) handler);
	}

	public <T> TraversalContext registerHandler(PersistentProperty<?> property, Class<T> type,
			Function<? super T, Object> handler) {

		Assert.isTrue(type.isAssignableFrom(property.getType()), () -> String
				.format("Cannot register a property handler for %s on a property of type %s!", type, property.getType()));

		Function<Object, T> caster = it -> type.cast(it);

		return registerHandler(property, caster.andThen(handler));
	}

	@Nullable
	Object postProcess(PersistentProperty<?> property, @Nullable Object value) {

		Function<Object, Object> handler = handlers.get(property);

		return handler == null ? value : handler.apply(value);
	}
}
