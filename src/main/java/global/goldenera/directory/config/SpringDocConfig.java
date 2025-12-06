/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025-2030 The GoldenEraGlobal Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package global.goldenera.directory.config;

import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SpringDocConfig {

	static {
		for (GlobalTypeRegistry.StringTypeAdapter<?> adapter : GlobalTypeRegistry.STRING_ADAPTERS) {
			SpringDocUtils.getConfig().replaceWithClass(adapter.getType(), String.class);
		}
		SpringDocUtils.getConfig().replaceWithClass(BigDecimal.class, String.class);
		SpringDocUtils.getConfig().replaceWithClass(BigInteger.class, String.class);
	}

	// --- API GROUPS ---
	@Bean
	public GroupedOpenApi coreApi() {
		return GroupedOpenApi.builder().group("CORE API")
				.pathsToMatch("/api/v1/identity", "/api/v1/identity/**", "/api/v1/node", "/api/v1/node/**")
				.build();
	}

	@Bean
	public GroupedOpenApi cryptoJApi() {
		return GroupedOpenApi.builder().group("CryptoJ API").pathsToMatch("/api/v1/cryptoj", "/api/v1/cryptoj/**")
				.build();
	}

	// --- ENUM DOC GENERATOR ---
	@Bean
	public ModelConverter enumSchemaConverter() {
		return new ModelConverter() {
			@Override
			public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
				JavaType javaType = Json.mapper().constructType(type.getType());
				if (javaType != null && javaType.isEnumType()) {
					Class<?> cls = javaType.getRawClass();
					if (GlobalTypeRegistry.CODE_ENUMS.contains(cls)) {
						return createEnumSchema(cls);
					}
				}
				if (chain.hasNext())
					return chain.next().resolve(type, context, chain);
				return null;
			}
		};
	}

	private Schema createEnumSchema(Class<?> cls) {
		IntegerSchema schema = new IntegerSchema();
		Object[] constants = cls.getEnumConstants();
		StringBuilder description = new StringBuilder();
		description.append("<b>Enum Values:</b><br>");
		description.append("<table border=\"1\" style=\"border-collapse: collapse;\">");
		description.append(
				"<thead><tr><th style=\"padding: 4px;\">Code</th><th style=\"padding: 4px;\">Name</th></tr></thead>");
		description.append("<tbody>");

		try {
			for (Object obj : constants) {
				int code = (int) cls.getMethod("getCode").invoke(obj);
				String name = ((Enum<?>) obj).name();

				description.append(String.format(
						"<tr><td style=\"padding: 4px;\"><code>%d</code></td><td style=\"padding: 4px;\">%s</td></tr>",
						code, name));
			}
		} catch (Exception e) {
		}

		description.append("</tbody></table>");

		schema.setDescription(description.toString());

		try {
			int firstCode = (int) cls.getMethod("getCode").invoke(constants[0]);
			schema.setExample(firstCode);
		} catch (Exception e) {
		}

		return schema;
	}
}