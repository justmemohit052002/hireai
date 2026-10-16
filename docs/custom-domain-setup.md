# Custom Domain Connection & Pre-Launch Runbook

This guide details the exact steps to connect your custom domain to HireAI (Frontend and Backend API) for production launch.

---

## 1. Domain Architecture

| Component | Target URL | Routing |
| :--- | :--- | :--- |
| **Frontend Web App** | `app.yourdomain.com` or `yourdomain.com` | Static hosting (Vercel, Cloudflare Pages, AWS CloudFront, or NGINX) |
| **Spring Boot API** | `api.yourdomain.com` | Reverse Proxy / Load Balancer to Spring Boot (:8080) |
| **FastAPI AI Engine** | Internal / `ai.yourdomain.com` | Internal microservice (:8000) accessible by Spring Boot |

---

## 2. DNS Record Configuration

In your DNS provider (Cloudflare, Namecheap, GoDaddy, AWS Route 53):

### A. Root / Frontend Subdomain
- **Type**: `CNAME`
- **Name**: `@` or `app`
- **Value**: Your hosting provider endpoint (e.g. `cname.vercel-dns.com` or server public IP)
- **Proxy Status**: Proxied (Cloudflare) or DNS only
- **TTL**: Auto / 300s

### B. Backend API Subdomain
- **Type**: `A` or `CNAME`
- **Name**: `api`
- **Value**: Server Elastic IP or ALB DNS
- **Proxy Status**: Proxied (Cloudflare)
- **TTL**: Auto / 300s

---

## 3. SSL / TLS Certificates

- **Automated**: If using Cloudflare or Vercel, SSL/TLS is provisioned automatically with TLS 1.3 encryption.
- **Certbot (Self-Hosted / NGINX)**:
  ```bash
  sudo certbot --nginx -d app.yourdomain.com -d api.yourdomain.com
  ```

---

## 4. Environment Variables & CORS Alignment

### A. Frontend (`hireai-frontend/.env.production`)
```env
VITE_API_URL=https://api.yourdomain.com
VITE_GOOGLE_CLIENT_ID=902036029804-64f1a7fp4h1bc2ctb77mja3v392bkdu8.apps.googleusercontent.com
VITE_SHOW_ERROR_DETAILS=false
```

### B. Backend (`hireai-backend/src/main/resources/application.properties`)
Update `cors.allowed-origins` to include your live domain:
```properties
cors.allowed-origins=https://yourdomain.com,https://app.yourdomain.com,https://www.yourdomain.com
```

### C. Google OAuth Authorized Origins
In Google Cloud Console:
1. Navigate to **APIs & Services > Credentials > OAuth 2.0 Client IDs**.
2. Under **Authorized JavaScript origins**, add:
   - `https://yourdomain.com`
   - `https://app.yourdomain.com`
3. Under **Authorized redirect URIs**, add:
   - `https://yourdomain.com/login`
   - `https://app.yourdomain.com/login`

---

## 5. Pre-Launch Verification Checklist

- [x] Favicon configured: Professional SVG neural node (no purple lightning bolts)
- [x] Zero "Made with AI" tags across codebase
- [x] Privacy Policy live and routed at `/privacy`
- [x] Terms and Conditions live and routed at `/terms`
- [x] All pill-shaped buttons removed
- [x] Zero purple gradients or arbitrary purple styling
- [x] Zero fake reviews, fake customer counters, or fabricated stats
- [x] Zero emojis in UI or notifications
- [x] Zero em dashes in UI copy
