import { db } from "./firebase";
import { doc, setDoc, collection, Timestamp, addDoc } from "firebase/firestore";

export const seedDashboardData = async () => {
  try {
    console.log("Starting dashboard seeding...");

    // 1. Seed Users (Sellers)
    const sellers = [
      {
        userId: "owner-1",
        fullName: "Rajesh Kumar",
        email: "rajesh.kumar@verdant.com",
        phone: "+94 77 123 4567",
        role: "seller",
        profileImageUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=500&auto=format&fit=crop",
        address: "123 Green Lane, Colombo 3",
        billing: {
          firstName: "Rajesh",
          lastName: "Kumar",
          email: "rajesh.kumar@verdant.com",
          phone: "+94 77 123 4567",
          address: "123 Green Lane",
          city: "Colombo",
          country: "Sri Lanka",
          postalCode: "00300"
        },
        shipping: {
          firstName: "Rajesh",
          lastName: "Kumar",
          email: "rajesh.kumar@verdant.com",
          phone: "+94 77 123 4567",
          address: "123 Green Lane",
          city: "Colombo",
          country: "Sri Lanka",
          postalCode: "00300"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-1",
        fcmToken: "fcm_seller_1",
        biometricEnabled: true,
        isActive: true,
        createdAt: Timestamp.now(),
      },
      {
        userId: "owner-2",
        fullName: "Priya Sharma",
        email: "priya.sharma@verdant.com",
        phone: "+94 77 987 6543",
        role: "seller",
        profileImageUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=500&auto=format&fit=crop",
        address: "456 Flower Street, Kandy",
        billing: {
          firstName: "Priya",
          lastName: "Sharma",
          email: "priya.sharma@verdant.com",
          phone: "+94 77 987 6543",
          address: "456 Flower Street",
          city: "Kandy",
          country: "Sri Lanka",
          postalCode: "20000"
        },
        shipping: {
          firstName: "Priya",
          lastName: "Sharma",
          email: "priya.sharma@verdant.com",
          phone: "+94 77 987 6543",
          address: "456 Flower Street",
          city: "Kandy",
          country: "Sri Lanka",
          postalCode: "20000"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-2",
        fcmToken: "fcm_seller_2",
        biometricEnabled: false,
        isActive: true,
        createdAt: Timestamp.now(),
      },
      {
        userId: "owner-3",
        fullName: "Aminda Jayasekara",
        email: "aminda.jayasekara@verdant.com",
        phone: "+94 71 234 5678",
        role: "seller",
        profileImageUrl: "https://images.unsplash.com/photo-1517458269450-691498d3d72a?q=80&w=500&auto=format&fit=crop",
        address: "789 Plant Avenue, Galle",
        billing: {
          firstName: "Aminda",
          lastName: "Jayasekara",
          email: "aminda.jayasekara@verdant.com",
          phone: "+94 71 234 5678",
          address: "789 Plant Avenue",
          city: "Galle",
          country: "Sri Lanka",
          postalCode: "80000"
        },
        shipping: {
          firstName: "Aminda",
          lastName: "Jayasekara",
          email: "aminda.jayasekara@verdant.com",
          phone: "+94 71 234 5678",
          address: "789 Plant Avenue",
          city: "Galle",
          country: "Sri Lanka",
          postalCode: "80000"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-3",
        fcmToken: "fcm_seller_3",
        biometricEnabled: true,
        isActive: true,
        createdAt: Timestamp.now(),
      }
    ];

    for (const seller of sellers) {
      await setDoc(doc(db, "users", seller.userId), seller);
    }

    // 2. Seed Users (Customers)
    const customers = [
      {
        userId: "user-1",
        fullName: "Anuradhya Silva",
        email: "anuradhya.silva@email.com",
        phone: "+94 71 111 1111",
        role: "customer",
        profileImageUrl: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=500&auto=format&fit=crop",
        address: "45 Palm Road, Colombo 4",
        billing: {
          firstName: "Anuradhya",
          lastName: "Silva",
          email: "anuradhya.silva@email.com",
          phone: "+94 71 111 1111",
          address: "45 Palm Road",
          city: "Colombo",
          country: "Sri Lanka",
          postalCode: "00400"
        },
        shipping: {
          firstName: "Anuradhya",
          lastName: "Silva",
          email: "anuradhya.silva@email.com",
          phone: "+94 71 111 1111",
          address: "45 Palm Road",
          city: "Colombo",
          country: "Sri Lanka",
          postalCode: "00400"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-c1",
        fcmToken: "fcm_customer_1",
        biometricEnabled: true,
        isActive: true,
        createdAt: Timestamp.now(),
      },
      {
        userId: "user-2",
        fullName: "Malinda Fernando",
        email: "malinda.fernando@email.com",
        phone: "+94 72 222 2222",
        role: "customer",
        profileImageUrl: "https://images.unsplash.com/photo-1507209488894-9b3f2a9a3e5e?q=80&w=500&auto=format&fit=crop",
        address: "78 Main Street, Matara",
        billing: {
          firstName: "Malinda",
          lastName: "Fernando",
          email: "malinda.fernando@email.com",
          phone: "+94 72 222 2222",
          address: "78 Main Street",
          city: "Matara",
          country: "Sri Lanka",
          postalCode: "81000"
        },
        shipping: {
          firstName: "Malinda",
          lastName: "Fernando",
          email: "malinda.fernando@email.com",
          phone: "+94 72 222 2222",
          address: "12 Rose Lane",
          city: "Matara",
          country: "Sri Lanka",
          postalCode: "81000"
        },
        sameAsBilling: false,
        defaultAddressId: "addr-c2",
        fcmToken: "fcm_customer_2",
        biometricEnabled: false,
        isActive: true,
        createdAt: Timestamp.now(),
      },
      {
        userId: "user-3",
        fullName: "Sandeep Perera",
        email: "sandeep.perera@email.com",
        phone: "+94 73 333 3333",
        role: "customer",
        profileImageUrl: "https://images.unsplash.com/photo-1517504199306-c2a7b5f1e4b1?q=80&w=500&auto=format&fit=crop",
        address: "56 Garden Way, Negombo",
        billing: {
          firstName: "Sandeep",
          lastName: "Perera",
          email: "sandeep.perera@email.com",
          phone: "+94 73 333 3333",
          address: "56 Garden Way",
          city: "Negombo",
          country: "Sri Lanka",
          postalCode: "11500"
        },
        shipping: {
          firstName: "Sandeep",
          lastName: "Perera",
          email: "sandeep.perera@email.com",
          phone: "+94 73 333 3333",
          address: "56 Garden Way",
          city: "Negombo",
          country: "Sri Lanka",
          postalCode: "11500"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-c3",
        fcmToken: "fcm_customer_3",
        biometricEnabled: true,
        isActive: true,
        createdAt: Timestamp.now(),
      },
      {
        userId: "user-4",
        fullName: "Ravi Karunarathne",
        email: "ravi.karunarathne@email.com",
        phone: "+94 74 444 4444",
        role: "customer",
        profileImageUrl: "https://images.unsplash.com/photo-1516622671519-a5e3b69db900?q=80&w=500&auto=format&fit=crop",
        address: "34 Lotus Lane, Jaffna",
        billing: {
          firstName: "Ravi",
          lastName: "Karunarathne",
          email: "ravi.karunarathne@email.com",
          phone: "+94 74 444 4444",
          address: "34 Lotus Lane",
          city: "Jaffna",
          country: "Sri Lanka",
          postalCode: "40000"
        },
        shipping: {
          firstName: "Ravi",
          lastName: "Karunarathne",
          email: "ravi.karunarathne@email.com",
          phone: "+94 74 444 4444",
          address: "34 Lotus Lane",
          city: "Jaffna",
          country: "Sri Lanka",
          postalCode: "40000"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-c4",
        fcmToken: "fcm_customer_4",
        biometricEnabled: false,
        isActive: true,
        createdAt: Timestamp.now(),
      },
      {
        userId: "user-5",
        fullName: "Kavya Wickrama",
        email: "kavya.wickrama@email.com",
        phone: "+94 75 555 5555",
        role: "customer",
        profileImageUrl: "https://images.unsplash.com/photo-1514888286974-6c03bf1a7d5e?q=80&w=500&auto=format&fit=crop",
        address: "90 Vine Street, Ratnapura",
        billing: {
          firstName: "Kavya",
          lastName: "Wickrama",
          email: "kavya.wickrama@email.com",
          phone: "+94 75 555 5555",
          address: "90 Vine Street",
          city: "Ratnapura",
          country: "Sri Lanka",
          postalCode: "70000"
        },
        shipping: {
          firstName: "Kavya",
          lastName: "Wickrama",
          email: "kavya.wickrama@email.com",
          phone: "+94 75 555 5555",
          address: "90 Vine Street",
          city: "Ratnapura",
          country: "Sri Lanka",
          postalCode: "70000"
        },
        sameAsBilling: true,
        defaultAddressId: "addr-c5",
        fcmToken: "fcm_customer_5",
        biometricEnabled: true,
        isActive: true,
        createdAt: Timestamp.now(),
      }
    ];

    for (const customer of customers) {
      await setDoc(doc(db, "users", customer.userId), customer);
    }

    // 3. Seed Nurseries
    const nurseries = [
      {
        nurseryId: "nursery-1",
        nurseryName: "Green Valley Nursery",
        ownerId: "owner-1",
        description: "Specializing in indoor tropical plants and succulents.",
        bannerImageUrl: "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1000&auto=format&fit=crop",
        phoneNumber: "+94 77 123 4567",
        latitude: 6.9271,
        longitude: 79.8612,
        businessHours: { mon: "08:00-18:00", tue: "08:00-18:00", wed: "08:00-18:00", thu: "08:00-18:00", fri: "08:00-18:00", sat: "09:00-16:00", sun: "Closed" },
        ratingAverage: 4.8,
        totalReviews: 124,
        createdAt: Timestamp.now(),
      },
      {
        nurseryId: "nursery-2",
        nurseryName: "Bloom & Grow",
        ownerId: "owner-2",
        description: "Your local source for flowering plants and landscaping shrubs.",
        bannerImageUrl: "https://images.unsplash.com/photo-1592150621344-82839b6fc236?q=80&w=1000&auto=format&fit=crop",
        phoneNumber: "+94 77 987 6543",
        latitude: 6.848,
        longitude: 79.9265,
        businessHours: { mon: "07:30-17:30", tue: "07:30-17:30", wed: "07:30-17:30", thu: "07:30-17:30", fri: "07:30-17:30", sat: "08:00-17:00", sun: "09:00-12:00" },
        ratingAverage: 4.5,
        totalReviews: 89,
        createdAt: Timestamp.now(),
      }
    ];

    for (const n of nurseries) {
      await setDoc(doc(db, "nurseries", n.nurseryId), n);
    }

    // 4. Seed Products
    const products = [
      {
        productId: "p1",
        name: "Monstera Deliciosa",
        category: "Indoor",
        price: 4500,
        oldPrice: 5200,
        stock: 45,
        available: true,
        nurseryId: "nursery-1",
        imageUrls: ["https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=500&auto=format&fit=crop"],
        createdAt: Timestamp.now(),
        soldCount: 12,
        rating: 4.9
      },
      {
        productId: "p2",
        name: "Snake Plant",
        category: "Indoor",
        price: 2200,
        oldPrice: 2500,
        stock: 120,
        available: true,
        nurseryId: "nursery-1",
        imageUrls: ["https://images.unsplash.com/photo-1593482892290-f54927ae1ebb?q=80&w=500&auto=format&fit=crop"],
        createdAt: Timestamp.now(),
        soldCount: 45,
        rating: 4.7
      },
      {
        productId: "p3",
        name: "Bird of Paradise",
        category: "Outdoor",
        price: 8500,
        oldPrice: 9000,
        stock: 15,
        available: true,
        nurseryId: "nursery-2",
        imageUrls: ["https://images.unsplash.com/photo-1544860707-c352cc5a92e3?q=80&w=500&auto=format&fit=crop"],
        createdAt: Timestamp.now(),
        soldCount: 8,
        rating: 4.8
      }
    ];

    for (const p of products) {
      await setDoc(doc(db, "products", p.productId), p);
    }

    // 5. Seed Orders
    const orders = [
      {
        userId: "user-1",
        items: [
          { productId: "p1", productName: "Monstera Deliciosa", productPrice: 4500, quantity: 1, productImage: "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=500&auto=format&fit=crop" }
        ],
        totalAmount: 4500,
        status: "DELIVERED",
        createdAt: Timestamp.now(),
        address: "78, Main St, Colombo",
        paymentMethod: "CARD"
      },
      {
        userId: "user-2",
        items: [
          { productId: "p2", productName: "Snake Plant", productPrice: 2200, quantity: 2, productImage: "https://images.unsplash.com/photo-1593482892290-f54927ae1ebb?q=80&w=500&auto=format&fit=crop" }
        ],
        totalAmount: 4400,
        status: "PROCESSING",
        createdAt: Timestamp.now(),
        address: "12, Rose Lane, Kandy",
        paymentMethod: "CASH"
      }
    ];

    for (const o of orders) {
      await addDoc(collection(db, "orders"), o);
    }

    console.log("Dashboard seeding completed successfully!");
    return true;
  } catch (error) {
    console.error("Error seeding dashboard data:", error);
    return false;
  }
};
