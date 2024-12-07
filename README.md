# SkedEat - Restaurant Booking Platform
![Restaurant Image](https://res.cloudinary.com/dpysbryyk/image/upload/v1733591854/sxwygboffhhqxccrxxqm.jpg)

SkedEat là Nền tảng đặt bàn trực tuyến kết nối khách hàng với nhà hàng một cách nhanh chóng, đồng thời hỗ trợ nhà hàng quản lý tối ưu quy trình và doanh thu.

## Tính Năng của SkedEat

### **Guest (Khách)**
Khách tham quan là những người dùng chưa đăng ký hoặc đăng nhập vào SkedEat. Họ có thể duyệt các nhà hàng nhưng có quyền truy cập hạn chế vào một số tính năng so với người dùng đã đăng ký.

- Duyệt nhà hàng gần đó dựa trên vị trí và độ phổ biến
- Tìm kiếm nhà hàng bằng giọng nói
- Xem thực đơn của nhà hàng
- Xem thông tin chi tiết về nhà hàng
- Xem đánh giá của nhà hàng
- Đăng ký tài khoản

### **User (Người Dùng Đã Đăng Ký)**
Người dùng đã đăng ký có thể tham gia vào các tính năng của SkedEat để tương tác với các nhà hàng và quản lý các đặt chỗ.

- Quản lý thông tin hồ sơ người dùng
- Đặt bàn tại các nhà hàng đã chọn
- Đặt món ăn trước khi đến nhà hàng khi đặt bàn
- Thanh toán bằng QR Code
- Lọc và xem lịch sử đặt chỗ
- Đưa ra hoặc chỉnh sửa đánh giá cho nhà hàng về việc đặt bàn
- Nhận email tự động thông báo về các đặt chỗ mới, đã xác nhận và sắp tới

### **Restaurant (Nhà Hàng)**
Các chủ sở hữu hoặc quản lý nhà hàng có thể quản lý thông tin và các chức năng liên quan đến nhà hàng của họ trên SkedEat.

- Quản lý thông tin hồ sơ nhà hàng
- Quản lý thực đơn và giá của nhà hàng
- Quản lý các gói quảng cáo đã mua của nhà hàng
- Xem, chấp nhận hoặc từ chối các đặt chỗ và thông báo cho khách hàng
- Theo dõi thống kê đặt chỗ của nhà hàng
- Xem, phản hồi và báo cáo đánh giá của khách hàng
- Theo dõi phí với nhắc nhở thanh toán hàng tháng

## Công Nghệ Sử Dụng

- **Backend**: 
  - **RESTful API** sử dụng **Spring Boot**
  - **MySQL**
  - **JWT** (JSON Web Token) cho xác thực người dùng
  - **PayOS** cho xử lý thanh toán qua QR Code
  - **Dockerfile** phục vụ cho việc deploy ở **Render**

- **Frontend**:
  - **React** cho giao diện người dùng
  - **Redux** cho quản lý trạng thái ứng dụng
  - **TailwindCSS** cho thiết kế giao diện
   
- Link: [SkedEat-FE](https://github.com/NTDung93/restaurant-booking-platform-fe.git)


## Liên Kết Website

- Website chính thức: [SkedEat](https://www.skedeat.site)


