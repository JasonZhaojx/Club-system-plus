export default function JoinView() {
  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Contact Us</p>
          <h1>加入学联</h1>
          <p>有任何问题或想法，欢迎随时联系学联。家，因为有你。</p>
        </div>
      </div>

      <div className="contact-body public-contact-body">
        <div className="contact-cards">
          <div className="contact-card">
            <div className="c-icon">💬</div>
            <div>
              <div className="c-label">微信公众号</div>
              <div className="c-val">新南CSA</div>
            </div>
          </div>
          <div className="contact-card">
            <div className="c-icon">📱</div>
            <div>
              <div className="c-label">小红书</div>
              <div className="c-val">新南CSA / UNSW CSA</div>
            </div>
          </div>
          <div className="contact-card">
            <div className="c-icon">📸</div>
            <div>
              <div className="c-label">Instagram</div>
              <div className="c-val">@unswcsa</div>
            </div>
          </div>
          <div className="contact-card">
            <div className="c-icon">📧</div>
            <div>
              <div className="c-label">邮箱</div>
              <div className="c-val">unswcsa@gmail.com</div>
            </div>
          </div>
          <div className="contact-card">
            <div className="c-icon">📍</div>
            <div>
              <div className="c-label">地址</div>
              <div className="c-val">UNSW Sydney, Kensington NSW 2033</div>
            </div>
          </div>
        </div>

        <div className="join-form public-join-card">
          <h3>加入学联</h3>
          <p>
            新南学联长期欢迎对活动策划、宣传内容、外联合作、学术职业发展、体育社交和校园服务感兴趣的同学加入。
            你可以通过微信公众号、小红书或 Instagram 联系我们，了解最新招新安排。
          </p>
          <p>
            如果你希望参与活动志愿者、部门干事或合作咨询，也可以直接邮件联系学联。
          </p>
          <a className="primary-link" href="mailto:unswcsa@gmail.com">
            发送邮件
          </a>
        </div>
      </div>
    </section>
  )
}
