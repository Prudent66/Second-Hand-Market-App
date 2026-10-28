const User = require('./User');
const VendorDetails = require('./VendorDetails');
const Product = require('./Product');
const Order = require('./Order');

async function initDatabase() {
  console.log('Initializing database tables...');
  
  await User.createTable();
  console.log('✓ Users table ready');
  
  await VendorDetails.createTable();
  console.log('✓ VendorDetails table ready');
  
  await Product.createTable();
  console.log('✓ Products table ready');
  
  await Order.createTable();
  console.log('✓ Orders table ready');
  
  console.log('Database initialization complete!');
}

if (require.main === module) {
  initDatabase()
    .then(() => process.exit(0))
    .catch(err => {
      console.error('Database initialization failed:', err);
      process.exit(1);
    });
}

module.exports = { initDatabase };