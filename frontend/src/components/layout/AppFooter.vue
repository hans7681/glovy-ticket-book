<template>
  <footer class="app-footer">
    <div class="container">
      <div class="footer-content">
        <div class="footer-section">
          <h3>关于我们</h3>
          <p>
            Glovy
            光维娱乐是一个提供在线电影票预订、场次查询和影院信息的平台，致力于为用户提供便捷的购票体验。
          </p>
        </div>

        <div class="footer-section">
          <h3>快速链接</h3>
          <ul>
            <li><RouterLink to="/">首页</RouterLink></li>
            <li><RouterLink to="/movies">电影</RouterLink></li>
            <li><RouterLink to="/cinemas">影院</RouterLink></li>
            <li><RouterLink to="/about">关于</RouterLink></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>联系我们</h3>
          <p>电子邮件：m13838241914@163.com</p>
          <p>客服热线：400-123-4567</p>
        </div>
      </div>

      <div class="copyright">
        <p>&copy; {{ new Date().getFullYear() }} Glovy 光维娱乐. 保留所有权利.</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background-color: #1a1a1a;
  color: #f1f1f1;
  padding: 3rem 0 1.5rem;
  margin-top: auto;
  width: 100%;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  width: 100%;
}

.footer-content {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  margin-bottom: 2rem;
}

.footer-section {
  flex: 1;
  min-width: 250px;
  margin-bottom: 1.5rem;
  padding-right: 2rem;
}

.footer-section h3 {
  font-size: 1.2rem;
  margin-bottom: 1rem;
  color: #e50914;
}

.footer-section p {
  line-height: 1.5;
  color: #b3b3b3;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
}

.footer-section ul li {
  margin-right: 1.5rem;
  margin-bottom: 0.5rem;
}

.footer-section a {
  color: #b3b3b3;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-section a:hover {
  color: #e50914;
}

.copyright {
  text-align: center;
  padding-top: 1.5rem;
  border-top: 1px solid #333;
  color: #777;
  font-size: 0.9rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
  }

  .footer-section {
    width: 100%;
    padding-right: 0;
  }
}
</style>
