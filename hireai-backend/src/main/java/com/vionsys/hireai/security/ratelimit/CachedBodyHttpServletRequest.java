package com.vionsys.hireai.security.ratelimit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * HttpServletRequest wrapper that caches the raw request body in memory,
 * allowing the body to be inspected multiple times (e.g. by rate limiting filter and Spring @RequestBody).
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final Charset encoding;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        String charEncoding = request.getCharacterEncoding();
        this.encoding = charEncoding != null ? Charset.forName(charEncoding) : StandardCharsets.UTF_8;
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Synchronous processing
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), this.encoding));
    }

    public byte[] getCachedBody() {
        return this.cachedBody;
    }

    public String getBodyAsString() {
        return new String(this.cachedBody, this.encoding);
    }
}
