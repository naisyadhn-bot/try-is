# Gunakan image resmi Nginx
FROM nginx:latest

# Hapus konfigurasi default Nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copy konfigurasi custom
COPY nginx.conf /etc/nginx/conf.d/

# Copy semua file HTML ke folder Nginx
COPY . /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
