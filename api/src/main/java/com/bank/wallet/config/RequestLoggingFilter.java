package com.bank.wallet.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {

	/* Adds requester IP and user-agent to logs for traceability */

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			if (request instanceof HttpServletRequest httpRequest) {
				var ip = httpRequest.getHeader("X-Forwarded-For");
				if (ip == null || ip.isEmpty()) {
					ip = request.getRemoteAddr();
				}
				var userAgent = httpRequest.getHeader("User-Agent");
				MDC.put("ip", ip);
				MDC.put("userAgent", userAgent != null ? userAgent : "");
			}
			chain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
