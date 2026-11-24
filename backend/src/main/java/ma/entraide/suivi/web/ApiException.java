package ma.entraide.suivi.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Object data;

    public ApiException(HttpStatus status, String code) { this(status, code, null); }
    public ApiException(HttpStatus status, String code, Object data) {
        super(code, null, false, false);
        this.status = status; this.code = code; this.data = data;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public Object data() { return data; }
}
