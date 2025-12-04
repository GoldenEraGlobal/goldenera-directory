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
package global.goldenera.directory.filters;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;

import global.goldenera.directory.properties.PropertiesGeneralConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Component
@Order(0)
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ThrottlingFilter implements Filter {

	PropertiesGeneralConfig propertiesGeneralConfig;

	final Map<String, Bucket> ipBuckets = Caffeine.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS)
			.maximumSize(10_000)
			.<String, Bucket>build()
			.asMap();

	private Bucket createNewBucket() {
		return Bucket.builder()
				.addLimit(limit -> limit.capacity(propertiesGeneralConfig.getMaxRequestsPerIpAddressPerMinute())
						.refillGreedy(propertiesGeneralConfig.getMaxRequestsPerIpAddressPerMinute(),
								Duration.ofMinutes(1)))
				.build();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

		String requestURI = httpRequest.getRequestURI();

		if (!requestURI.startsWith("/api/") || requestURI.equals("/favicon.ico")) {
			filterChain.doFilter(servletRequest, servletResponse);
			return;
		}

		String ip = httpRequest.getRemoteAddr();
		Bucket bucket = ipBuckets.computeIfAbsent(ip, k -> createNewBucket());

		if (bucket.tryConsume(1)) {
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
			httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
			httpResponse.getWriter().write("{\"message\":\"Too many requests\"}");
		}
	}

}