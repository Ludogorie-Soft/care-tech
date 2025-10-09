# Tech Store API

A comprehensive Spring Boot REST API for a tech store, featuring complete product management, admin panel, and advanced e-commerce functionality.

## 🚀 Features

### Core Features
- **Complete Product Management** with detailed specifications
- **Flexible Discount System** (positive/negative values)
- **Category Management** with hierarchical structure
- **Brand Management** with featured brands
- **User Management** with role-based access control
- **JWT Authentication** and authorization
- **File Upload** for product images, category images, and brand logos
- **Advanced Search and Filtering**
- **RESTful API** with comprehensive endpoints

### Admin Panel
- **React-based Admin Interface**
- **Dashboard** with analytics and statistics
- **Product CRUD Operations** with specifications management
- **Category and Brand Management**
- **User Management**
- **Real-time Stock Monitoring**

### Technical Features
- **Spring Boot 3.2** with Java 17
- **PostgreSQL** database with JPA/Hibernate
- **JWT Token-based Authentication**
- **OpenAPI/Swagger** documentation
- **Docker** containerization
- **Comprehensive Error Handling**
- **Input Validation**
- **Audit Logging**
- **Caching Support**
- **File Upload Management**

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or use Docker)
- Docker & Docker Compose (optional but recommended)

## 🛠️ Installation & Setup

### Option 1: Docker (Recommended)

1. **Clone the repository**
```bash
git clone <repository-url>
cd tech-store-api
```

2. **Create environment file**
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Start with Docker Compose**
```bash
# Production
docker-compose up -d

# Development
docker-compose -f docker-compose.dev.yml up -d
```

### Option 2: Local Development

1. **Clone and setup**
```bash
git clone <repository-url>
cd tech-store-api
```

2. **Setup PostgreSQL Database**
```sql
CREATE DATABASE techstore;
CREATE USER techstore_user WITH PASSWORD 'techstore_password';
GRANT ALL PRIVILEGES ON DATABASE techstore TO techstore_user;
```

3. **Configure application**
```bash
# Copy and edit application.yml
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

4. **Run the application**
```bash
# Using Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/tech-store-api-1.0.0.jar
```

## 📚 API Documentation

### Swagger UI
Access the interactive API documentation at:
- **Local Development**: http://localhost:8080/swagger-ui.html
- **Production**: https://your-domain/swagger-ui.html

### Admin Panel
Access the admin panel at:
- **Local Development**: http://localhost:8080/admin/
- **Production**: https://your-domain/admin/

**Default Admin Credentials:**
- Username: `admin`
- Password: `admin123`


```

## 💾 Database Schema

### Key Entities
- **Products**: Complete product information with specifications
- **Categories**: Hierarchical category structure
- **Brands**: Brand information with logos
- **Users**: User accounts with roles
- **ProductSpecifications**: Detailed product specifications

### Product Specifications System
Products support unlimited specifications with:
- Specification name (e.g., "CPU", "RAM", "Storage")
- Specification value (e.g., "Intel i7", "16GB", "512GB")
- Unit (e.g., "GHz", "GB", "inches")
- Group (e.g., "Performance", "Display", "Connectivity")
- Sort order for display

### Discount System
The flexible discount system supports:
- **Negative values**: Discounts (e.g., -50 = $50 off)
- **Positive values**: Markups (e.g., +25 = $25 markup)
- **Automatic calculation**: Discounted price = base price + discount

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/techstore/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Exception handling
│   │   ├── repository/     # Data repositories
│   │   ├── service/        # Business logic
│   │   ├── util/           # Utility classes
│   │   └── TechStoreApplication.java
│   └── resources/
│       ├── static/admin/   # Admin panel files
│       ├── application.yml # Configuration
│       └── data.sql       # Sample data
└── test/                  # Test classes
```

## 🔧 Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/techstore
DATABASE_USERNAME=techstore_user
DATABASE_PASSWORD=techstore_password

# JWT
JWT_SECRET=your-jwt-secret-key

# Admin
ADMIN_PASSWORD=your-admin-password

# File Upload
UPLOAD_DIR=./uploads
```

### Application Profiles
- **development**: Local development with H2/PostgreSQL
- **test**: Testing configuration
- **production**: Production optimized settings

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# With coverage
./mvnw clean test jacoco:report
```

### Test Coverage
View coverage reports at: `target/site/jacoco/index.html`

## 📦 Deployment

### Docker Production Deployment
```bash
# Build and start
docker-compose -f docker-compose.yml up -d

# View logs
docker-compose logs -f app

# Scale services
docker-compose up -d --scale app=3
```

### Traditional Deployment
```bash
# Build JAR
./mvnw clean package -DskipTests

# Run with production profile
java -jar -Dspring.profiles.active=production target/tech-store-api-1.0.0.jar
```

## 🔒 Security

### Authentication
- JWT-based authentication
- Role-based access control (USER, ADMIN, SUPER_ADMIN)
- Password encryption with BCrypt
- Token expiration and refresh

### Authorization
- Method-level security with `@PreAuthorize`
- Public endpoints for product browsing
- Protected admin endpoints
- File upload restrictions

## 📈 Monitoring

### Health Checks
- Application health: `/actuator/health`
- Database connectivity
- Disk space monitoring

### Metrics
- Prometheus metrics: `/actuator/prometheus`
- JVM metrics
- Custom business metrics

## 🚀 Performance

### Caching
- Application-level caching
- Database query optimization
- Static file serving

### Database Optimization
- Proper indexing
- Query optimization
- Connection pooling with HikariCP

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Email: developers@techstore.com

## 🎯 Roadmap

### Upcoming Features
- [ ] Order management system
- [ ] Payment integration
- [ ] Inventory management
- [ ] Email notifications
- [ ] Advanced analytics
- [ ] Mobile API optimizations
- [ ] Real-time notifications
- [ ] Multi-language support

### Performance Improvements
- [ ] Redis caching
- [ ] Database sharding
- [ ] CDN integration
- [ ] API rate limiting

---

**Tech Store API** - Built with ❤️ using Spring Boot 3.2 and React